package com.atelie.sistema.precificacao;

import com.atelie.sistema.financeiro.DespesaRepository;
import com.atelie.sistema.ordemservico.RecebimentoRepository;
import com.atelie.sistema.precificacao.dto.ExecucaoPIDResponse;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import com.atelie.sistema.usuario.Usuario;
import com.atelie.sistema.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AtualizacaoDiariaPIDService {
    private final MetaMensalRepository metas;
    private final EstadoPIDRepository estados;
    private final ExecucaoPIDDiariaRepository execucoes;
    private final PoliticaPrecificacaoRepository politicas;
    private final RecebimentoRepository recebimentos;
    private final DespesaRepository despesas;
    private final UsuarioRepository usuarios;
    private final ControladorPID controlador;

    AtualizacaoDiariaPIDService(MetaMensalRepository metas, EstadoPIDRepository estados,
            ExecucaoPIDDiariaRepository execucoes, PoliticaPrecificacaoRepository politicas,
            RecebimentoRepository recebimentos, DespesaRepository despesas, UsuarioRepository usuarios,
            ControladorPID controlador) {
        this.metas = metas;
        this.estados = estados;
        this.execucoes = execucoes;
        this.politicas = politicas;
        this.recebimentos = recebimentos;
        this.despesas = despesas;
        this.usuarios = usuarios;
        this.controlador = controlador;
    }

    @Transactional
    ExecucaoPIDResponse atualizarHoje(String emailResponsavel) {
        return atualizar(LocalDate.now(), emailResponsavel);
    }

    @Transactional
    public void atualizarAutomaticamente() {
        // A meta identifica quem autorizou o acompanhamento; não há usuário fictício para a rotina.
        var meta = metas.buscarPorMes(YearMonth.now().atDay(1));
        if (meta.isEmpty() || !politicas.existsById(PoliticaPrecificacao.ID_UNICA)) return;
        atualizar(LocalDate.now(), meta.get().getResponsavel().getEmail());
    }

    @Transactional
    ExecucaoPIDResponse atualizar(LocalDate data, String emailResponsavel) {
        if (data == null || data.isAfter(LocalDate.now()))
            throw new RegraNegocioException("A atualização diária só pode ser executada para hoje ou para uma data passada.");
        YearMonth mes = YearMonth.from(data);
        LocalDate competencia = mes.atDay(1);
        MetaMensal meta = metas.bloquearPorMes(competencia).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cadastre a meta mensal antes de atualizar o PID."));

        var existente = execucoes.findByCompetenciaAndDataApuracao(competencia, data);
        if (existente.isPresent()) return resposta(existente.get());

        PoliticaPrecificacao politica = politicas.findById(PoliticaPrecificacao.ID_UNICA)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Configure a política de precificação antes de atualizar o PID."));
        Usuario responsavel = usuarios.findByEmail(emailResponsavel).orElseThrow(() ->
                new RecursoNaoEncontradoException("Usuário responsável não encontrado."));

        LocalDate inicio = mes.atDay(1);
        LocalDate fimExclusivo = data.plusDays(1);
        BigDecimal entradas = recebimentos.somarRecebimentos(inicio, fimExclusivo);
        BigDecimal saidas = despesas.somarPagamentos(inicio, fimExclusivo);
        BigDecimal realizado = zerar(entradas).subtract(zerar(saidas));
        double erro = controlador.normalizarErro(meta.getValorMeta(), realizado, data.getDayOfMonth(), mes.lengthOfMonth());
        EstadoPID estado = estados.findById(competencia).orElseGet(() -> new EstadoPID(mes));
        long deltaDias = estado.getUltimaAtualizacao() == null ? 1
                : ChronoUnit.DAYS.between(estado.getUltimaAtualizacao(), data);
        ResultadoPID resultado = controlador.calcular(erro, deltaDias, estado.getErroAnterior(),
                estado.getIntegralAcumulada(), politica);
        estado.registrar(resultado.integralAcumulada(), erro, data);
        estados.save(estado);

        BigDecimal referencia = meta.getValorMeta().multiply(BigDecimal.valueOf(data.getDayOfMonth()))
                .divide(BigDecimal.valueOf(mes.lengthOfMonth()), 2, java.math.RoundingMode.HALF_UP);
        ExecucaoPIDDiaria execucao = new ExecucaoPIDDiaria(mes, data, deltaDias, meta.getValorMeta(),
                realizado, referencia, resultado, politica, responsavel);
        return resposta(execucoes.save(execucao));
    }

    @Transactional(readOnly = true)
    List<ExecucaoPIDResponse> listar(YearMonth mes) {
        if (mes == null) throw new RegraNegocioException("Informe a competência para consultar o histórico PID.");
        return execucoes.findByCompetenciaOrderByDataApuracaoDesc(mes.atDay(1)).stream()
                .map(this::resposta).toList();
    }

    private BigDecimal zerar(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(2) : valor.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private ExecucaoPIDResponse resposta(ExecucaoPIDDiaria e) {
        return new ExecucaoPIDResponse(e.getId(), e.getMes(), e.getDataApuracao(), e.getDeltaDias(),
                e.getValorMeta(), e.getResultadoRealizado(), e.getReferenciaAcumulada(), e.getErroNormalizado(),
                e.getTermoProporcional(), e.getTermoIntegral(), e.getTermoDerivativo(), e.getSaidaBruta(),
                e.getAjusteLimitado(), e.getIntegralAcumulada(), e.getGanhoProporcional(), e.getGanhoIntegral(),
                e.getGanhoDerivativo(), e.getAjusteMinimo(), e.getAjusteMaximo(), e.getResponsavelId(),
                e.getResponsavelNome());
    }
}
