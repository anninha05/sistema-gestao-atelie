package com.atelie.sistema.precificacao;

import com.atelie.sistema.precificacao.agente.CalibradorGanhos;
import com.atelie.sistema.precificacao.agente.ExperimentoPrecificacao;
import com.atelie.sistema.precificacao.dto.CalibracaoGanhosResponse;
import com.atelie.sistema.precificacao.dto.MetaMensalRequest;
import com.atelie.sistema.precificacao.dto.MetaMensalResponse;
import com.atelie.sistema.precificacao.dto.PoliticaPrecificacaoRequest;
import com.atelie.sistema.precificacao.dto.PoliticaPrecificacaoResponse;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.usuario.Usuario;
import com.atelie.sistema.usuario.UsuarioRepository;
import java.time.YearMonth;
import java.math.BigDecimal;
import java.util.UUID;
import com.atelie.sistema.ordemservico.OrdemServicoRepository;
import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ConfiguracaoPrecificacaoService {
    private final MetaMensalRepository metas;
    private final PoliticaPrecificacaoRepository politicas;
    private final UsuarioRepository usuarios;
    private final OrdemServicoRepository ordens;
    private final PisoPrecoOrdemRepository pisos;

    ConfiguracaoPrecificacaoService(MetaMensalRepository metas, PoliticaPrecificacaoRepository politicas,
            UsuarioRepository usuarios, OrdemServicoRepository ordens,
            PisoPrecoOrdemRepository pisos) {
        this.metas = metas;
        this.politicas = politicas;
        this.usuarios = usuarios;
        this.ordens = ordens;
        this.pisos = pisos;
    }

    @Transactional(readOnly = true)
    MetaMensalResponse buscarMeta(YearMonth mes) {
        MetaMensal meta = metas.buscarPorMes(mes.atDay(1))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Meta mensal não cadastrada para " + mes + "."));
        return resposta(meta);
    }

    @Transactional
    MetaMensalResponse definirMeta(YearMonth mes, MetaMensalRequest request, String emailResponsavel) {
        Usuario responsavel = buscarResponsavel(emailResponsavel);
        if (!politicas.existsById(PoliticaPrecificacao.ID_UNICA)) {
            // Perfil técnico inicial para demonstração, sujeito à calibração.
            // O limite comercial de 20% foi definido para o fluxo simplificado.
            politicas.save(new PoliticaPrecificacao(0.2, 0.01, 0.05, -0.20, 0.20, null, null, responsavel));
        } else {
            var politica = politicas.findById(PoliticaPrecificacao.ID_UNICA).orElseThrow();
            politica.configurar(politica.getGanhoProporcional(), politica.getGanhoIntegral(),
                    politica.getGanhoDerivativo(), -0.20, 0.20, politica.getDescontoFidelidade(),
                    politica.getQuantidadeOsFidelidade(), responsavel);
        }

        MetaMensal meta = metas.buscarPorMes(mes.atDay(1)).orElseGet(() ->
                new MetaMensal(mes, request.valorMeta(), request.indicador(), responsavel));
        meta.alterar(request.valorMeta(), request.indicador(), responsavel);
        return resposta(metas.save(meta));
    }

    @Transactional(readOnly = true)
    PoliticaPrecificacaoResponse buscarPolitica() {
        return politicas.findById(PoliticaPrecificacao.ID_UNICA).map(this::resposta)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Política de precificação ainda não configurada."));
    }

    @Transactional
    PoliticaPrecificacaoResponse configurarPolitica(PoliticaPrecificacaoRequest request, String emailResponsavel) {
        Usuario responsavel = buscarResponsavel(emailResponsavel);
        PoliticaPrecificacao politica = politicas.findById(PoliticaPrecificacao.ID_UNICA)
                .orElseGet(() -> new PoliticaPrecificacao(request.ganhoProporcional(), request.ganhoIntegral(),
                        request.ganhoDerivativo(), request.ajusteMinimo(), request.ajusteMaximo(),
                        request.descontoFidelidade(), request.quantidadeOsFidelidade(), responsavel));
        politica.configurar(request.ganhoProporcional(), request.ganhoIntegral(), request.ganhoDerivativo(),
                request.ajusteMinimo(), request.ajusteMaximo(), request.descontoFidelidade(),
                request.quantidadeOsFidelidade(), responsavel);
        return resposta(politicas.save(politica));
    }

    @Transactional
    CalibracaoGanhosResponse calibrarGanhos(String emailResponsavel) {
        Usuario responsavel = buscarResponsavel(emailResponsavel);
        PoliticaPrecificacao politica = politicas.findById(PoliticaPrecificacao.ID_UNICA).orElseThrow(() ->
                new RecursoNaoEncontradoException("Configure a política de precificação antes de calibrar os ganhos."));
        CalibradorGanhos.Resultado treino = new ExperimentoPrecificacao().treinar(politica.getGanhos());
        ControladorPID.Ganhos novos = treino.calibrado();
        politica.configurar(novos.kp(), novos.ki(), novos.kd(), politica.getAjusteMinimo(), politica.getAjusteMaximo(),
                politica.getDescontoFidelidade(), politica.getQuantidadeOsFidelidade(), responsavel);
        ControladorPID.Ganhos anteriores = treino.inicial();
        return new CalibracaoGanhosResponse(anteriores.kp(), anteriores.ki(), anteriores.kd(), treino.custoInicial(),
                novos.kp(), novos.ki(), novos.kd(), treino.custoFinal(), treino.historicoCusto().size() - 1);
    }

    @Transactional(readOnly = true)
    BigDecimal buscarPiso(UUID ordemId) {
        return pisos.findById(ordemId).map(PisoPrecoOrdem::getValor).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cadastre o preço mínimo desta ordem antes de consultar sugestões."));
    }

    @Transactional
    BigDecimal salvarPiso(UUID ordemId, BigDecimal valor, String email) {
        var ordem = ordens.buscarParaAtualizacao(ordemId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Ordem de serviço não encontrada."));
        if (ordem.getStatusAtual() != StatusOrdemServico.RESERVADA)
            throw new RegraNegocioException("O preço mínimo só pode ser configurado em uma ordem reservada.");
        return pisos.save(new PisoPrecoOrdem(ordemId, valor, buscarResponsavel(email))).getValor();
    }

    private Usuario buscarResponsavel(String email) {
        return usuarios.findByEmail(email).orElseThrow(() ->
                new RecursoNaoEncontradoException("Usuário responsável não encontrado."));
    }

    private MetaMensalResponse resposta(MetaMensal meta) {
        return new MetaMensalResponse(meta.getMes(), meta.getValorMeta(), meta.getIndicador(),
                meta.getResponsavel().getId(), meta.getResponsavel().getNome());
    }

    private PoliticaPrecificacaoResponse resposta(PoliticaPrecificacao politica) {
        return new PoliticaPrecificacaoResponse(politica.getGanhoProporcional(), politica.getGanhoIntegral(),
                politica.getGanhoDerivativo(), politica.getAjusteMinimo(), politica.getAjusteMaximo(),
                politica.getDescontoFidelidade(), politica.getQuantidadeOsFidelidade(),
                politica.getResponsavel().getId(), politica.getResponsavel().getNome(), politica.getAtualizadaEm());
    }
}
