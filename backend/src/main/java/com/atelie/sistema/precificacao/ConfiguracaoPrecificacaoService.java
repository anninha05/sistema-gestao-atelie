package com.atelie.sistema.precificacao;

import com.atelie.sistema.precificacao.dto.MetaMensalRequest;
import com.atelie.sistema.precificacao.dto.MetaMensalResponse;
import com.atelie.sistema.precificacao.dto.PoliticaPrecificacaoRequest;
import com.atelie.sistema.precificacao.dto.PoliticaPrecificacaoResponse;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.usuario.Usuario;
import com.atelie.sistema.usuario.UsuarioRepository;
import java.time.YearMonth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ConfiguracaoPrecificacaoService {
    private final MetaMensalRepository metas;
    private final PoliticaPrecificacaoRepository politicas;
    private final UsuarioRepository usuarios;

    ConfiguracaoPrecificacaoService(MetaMensalRepository metas, PoliticaPrecificacaoRepository politicas,
            UsuarioRepository usuarios) {
        this.metas = metas;
        this.politicas = politicas;
        this.usuarios = usuarios;
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
