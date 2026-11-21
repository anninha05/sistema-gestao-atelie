package com.atelie.sistema.precificacao;

import com.atelie.sistema.financeiro.DespesaRepository;
import com.atelie.sistema.ordemservico.RecebimentoRepository;
import com.atelie.sistema.precificacao.PrecificacaoDTO.CalibracaoResponse;
import com.atelie.sistema.precificacao.PrecificacaoDTO.ExecucaoResponse;
import com.atelie.sistema.precificacao.PrecificacaoDTO.MetaRequest;
import com.atelie.sistema.precificacao.PrecificacaoDTO.MetaResponse;
import com.atelie.sistema.precificacao.PrecificacaoDTO.PoliticaRequest;
import com.atelie.sistema.precificacao.PrecificacaoDTO.PoliticaResponse;
import com.atelie.sistema.precificacao.agente.AgentePrecificacao;
import com.atelie.sistema.precificacao.agente.CalibradorGanhos;
import com.atelie.sistema.precificacao.agente.ExperimentoPrecificacao;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import com.atelie.sistema.usuario.Usuario;
import com.atelie.sistema.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class PrecificacaoService {

    private static final Logger LOG = LoggerFactory.getLogger(PrecificacaoService.class);

    private final MetaMensalRepository metas;
    private final PoliticaPrecificacaoRepository politicas;
    private final ExecucaoPIDDiariaRepository execucoes;
    private final RecebimentoRepository recebimentos;
    private final DespesaRepository despesas;
    private final UsuarioRepository usuarios;
    private final ControladorPID controlador;
    private final TransactionTemplate transacao;
    private final boolean agendamentoAtivo;

    PrecificacaoService(MetaMensalRepository metas, PoliticaPrecificacaoRepository politicas,
            ExecucaoPIDDiariaRepository execucoes, RecebimentoRepository recebimentos, DespesaRepository despesas,
            UsuarioRepository usuarios, ControladorPID controlador, TransactionTemplate transacao,
            @Value("${precificacao.agendamento.ativo:true}") boolean agendamentoAtivo) {
        this.metas = metas;
        this.politicas = politicas;
        this.execucoes = execucoes;
        this.recebimentos = recebimentos;
        this.despesas = despesas;
        this.usuarios = usuarios;
        this.controlador = controlador;
        this.transacao = transacao;
        this.agendamentoAtivo = agendamentoAtivo;
    }

    @Transactional(readOnly = true)
    MetaResponse buscarMeta(YearMonth mes) {
        return metas.buscarPorMes(mes.atDay(1)).map(MetaResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Meta mensal não cadastrada para " + mes + "."));
    }

    @Transactional
    MetaResponse definirMeta(YearMonth mes, MetaRequest request, String email) {
        if (request == null) throw new RegraNegocioException("Informe o valor da meta mensal.");
        Usuario responsavel = buscarResponsavel(email);
        PoliticaPrecificacao politica = politicas.findById(PoliticaPrecificacao.ID_UNICA)
                .orElseGet(() -> politicas.save(politicaInicial(responsavel)));
        MetaMensal meta = metas.buscarPorMes(mes.atDay(1)).orElseGet(() ->
                new MetaMensal(mes, request.valorMeta(), responsavel));
        meta.alterar(request.valorMeta(), responsavel);
        metas.save(meta);
        if (mes.equals(YearMonth.now())) {
            atualizar(LocalDate.now(), meta, politica, responsavel);
        }
        return MetaResponse.de(meta);
    }

    @Transactional(readOnly = true)
    PoliticaResponse buscarPolitica() {
        return PoliticaResponse.de(buscarPoliticaConfigurada());
    }

    @Transactional
    PoliticaResponse configurarPolitica(PoliticaRequest request, String email) {
        if (request == null) throw new RegraNegocioException("Informe a política de precificação.");
        Usuario responsavel = buscarResponsavel(email);
        PoliticaPrecificacao politica = politicas.findById(PoliticaPrecificacao.ID_UNICA)
                .orElseGet(() -> politicaInicial(responsavel));
        politica.configurar(request.ganhoProporcional(), request.ganhoIntegral(), request.ganhoDerivativo(),
                request.ajusteMinimo(), request.ajusteMaximo(), request.descontoFidelidade(),
                request.quantidadeOsFidelidade(), responsavel);
        return PoliticaResponse.de(politicas.save(politica));
    }

    @Transactional
    CalibracaoResponse calibrarGanhos(String email) {
        Usuario responsavel = buscarResponsavel(email);
        PoliticaPrecificacao politica = buscarPoliticaConfigurada();
        CalibradorGanhos.Resultado treino = new ExperimentoPrecificacao().treinar(politica.getGanhos());
        ControladorPID.Ganhos anteriores = treino.inicial();
        ControladorPID.Ganhos novos = treino.calibrado();
        politica.configurar(novos.kp(), novos.ki(), novos.kd(), politica.getAjusteMinimo(), politica.getAjusteMaximo(),
                politica.getDescontoFidelidade(), politica.getQuantidadeOsFidelidade(), responsavel);
        return new CalibracaoResponse(anteriores.kp(), anteriores.ki(), anteriores.kd(), treino.custoInicial(),
                novos.kp(), novos.ki(), novos.kd(), treino.custoFinal(), treino.historicoCusto().size() - 1);
    }

    @Scheduled(initialDelay = 10_000, fixedDelay = 3_600_000)
    void atualizarPeriodicamente() {
        if (!agendamentoAtivo) return;
        try {
            transacao.executeWithoutResult(status -> atualizarHoje());
        } catch (RuntimeException falha) {
            LOG.error("Falha ao atualizar a recomendação de preço; nova tentativa no próximo ciclo.", falha);
        }
    }

    @Transactional
    void atualizarHoje() {
        Optional<MetaMensal> meta = metas.bloquearPorMes(YearMonth.now().atDay(1));
        Optional<PoliticaPrecificacao> politica = politicas.findById(PoliticaPrecificacao.ID_UNICA);
        if (meta.isEmpty() || politica.isEmpty()) return;
        atualizar(LocalDate.now(), meta.get(), politica.get(), meta.get().getResponsavel());
    }

    @Transactional(readOnly = true)
    List<ExecucaoResponse> listarExecucoes(YearMonth mes) {
        if (mes == null) throw new RegraNegocioException("Informe a competência para consultar o histórico.");
        return execucoes.findByCompetenciaOrderByDataApuracaoDesc(mes.atDay(1)).stream()
                .map(ExecucaoResponse::de).toList();
    }

    Optional<ExecucaoPIDDiaria> ultimaExecucao(YearMonth mes) {
        return execucoes.findFirstByCompetenciaOrderByDataApuracaoDesc(mes.atDay(1));
    }

    private ExecucaoPIDDiaria atualizar(LocalDate data, MetaMensal meta, PoliticaPrecificacao politica,
            Usuario responsavel) {
        YearMonth mes = meta.getMes();
        LocalDate competencia = mes.atDay(1);
        Optional<ExecucaoPIDDiaria> existente = execucoes.findByCompetenciaAndDataApuracao(competencia, data);
        if (existente.isPresent()) return existente.get();

        BigDecimal realizado = zerar(recebimentos.somarRecebimentos(competencia, data.plusDays(1)))
                .subtract(zerar(despesas.somarPagamentos(competencia, data.plusDays(1))));
        Optional<ExecucaoPIDDiaria> anterior = ultimaExecucao(mes);
        long deltaDias = anterior.map(e -> ChronoUnit.DAYS.between(e.getDataApuracao(), data)).orElse(1L);
        AgentePrecificacao agente = anterior
                .map(e -> new AgentePrecificacao(controlador, politica.getGanhos(), e.getIntegralAcumulada(),
                        e.getErroNormalizado()))
                .orElseGet(() -> new AgentePrecificacao(controlador, politica.getGanhos()));
        ControladorPID.Resultado resultado = agente.agir(new AgentePrecificacao.Percepcao(meta.getValorMeta(),
                realizado, data.getDayOfMonth(), mes.lengthOfMonth(), deltaDias));
        BigDecimal referencia = meta.getValorMeta().multiply(BigDecimal.valueOf(data.getDayOfMonth()))
                .divide(BigDecimal.valueOf(mes.lengthOfMonth()), 2, RoundingMode.HALF_UP);
        return execucoes.save(new ExecucaoPIDDiaria(mes, data, deltaDias, meta.getValorMeta(), realizado,
                referencia, resultado, politica.getGanhos(), responsavel));
    }

    private PoliticaPrecificacao politicaInicial(Usuario responsavel) {
        ControladorPID.Ganhos ganhos = ExperimentoPrecificacao.GANHOS_MANUAIS;
        return new PoliticaPrecificacao(ganhos.kp(), ganhos.ki(), ganhos.kd(), ganhos.ajusteMinimo(),
                ganhos.ajusteMaximo(), null, null, responsavel);
    }

    private PoliticaPrecificacao buscarPoliticaConfigurada() {
        return politicas.findById(PoliticaPrecificacao.ID_UNICA).orElseThrow(() ->
                new RecursoNaoEncontradoException("Configure a política de precificação antes de continuar."));
    }

    private Usuario buscarResponsavel(String email) {
        return usuarios.findByEmail(email).orElseThrow(() ->
                new RecursoNaoEncontradoException("Usuário responsável não encontrado."));
    }

    private BigDecimal zerar(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(2) : valor.setScale(2, RoundingMode.HALF_UP);
    }
}
