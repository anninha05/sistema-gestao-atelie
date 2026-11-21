package com.atelie.sistema.precificacao;

import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.ordemservico.OrdemServicoRepository;
import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.precificacao.PrecificacaoDTO.SugestaoRequest;
import com.atelie.sistema.precificacao.PrecificacaoDTO.SugestaoResponse;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import com.atelie.sistema.usuario.Usuario;
import com.atelie.sistema.usuario.UsuarioRepository;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SugestaoPrecoService {

    private final OrdemServicoRepository ordens;
    private final SugestaoPrecoRepository sugestoes;
    private final PoliticaPrecificacaoRepository politicas;
    private final UsuarioRepository usuarios;
    private final CalculadoraSugestaoPreco calculadora;
    private final PrecificacaoService precificacao;

    SugestaoPrecoService(OrdemServicoRepository ordens, SugestaoPrecoRepository sugestoes,
            PoliticaPrecificacaoRepository politicas, UsuarioRepository usuarios,
            CalculadoraSugestaoPreco calculadora, PrecificacaoService precificacao) {
        this.ordens = ordens;
        this.sugestoes = sugestoes;
        this.politicas = politicas;
        this.usuarios = usuarios;
        this.calculadora = calculadora;
        this.precificacao = precificacao;
    }

    @Transactional
    SugestaoResponse sugerir(SugestaoRequest request, String email) {
        if (request == null || request.ordemServicoId() == null)
            throw new RegraNegocioException("Selecione a ordem para calcular uma sugestão de preço.");
        OrdemServico ordem = ordens.findById(request.ordemServicoId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Ordem de serviço não encontrada."));
        if (ordem.getStatusAtual() != StatusOrdemServico.RESERVADA)
            throw new RegraNegocioException("A sugestão só pode ser criada antes do início da produção ou entrega.");
        PoliticaPrecificacao politica = politicas.findById(PoliticaPrecificacao.ID_UNICA).orElseThrow(() ->
                new RecursoNaoEncontradoException("Configure a política de precificação antes de gerar sugestões."));
        Usuario responsavel = buscarResponsavel(email);

        precificacao.atualizarHoje();
        ExecucaoPIDDiaria execucao = precificacao.ultimaExecucao(YearMonth.now()).orElse(null);
        double ajuste = execucao == null ? 0 : execucao.getAjusteLimitado();

        List<OrdemServico> historico = ordens.findByClienteId(ordem.getCliente().getId());
        int concluidas = (int) historico.stream().filter(os -> os.getStatusAtual() == StatusOrdemServico.ENTREGUE
                || os.getStatusAtual() == StatusOrdemServico.DEVOLVIDA).count();
        int canceladas = (int) historico.stream()
                .filter(os -> os.getStatusAtual() == StatusOrdemServico.CANCELADA).count();
        CalculadoraSugestaoPreco.Calculo calculo = calculadora.calcular(ordem.getValorTotal(), ajuste,
                politica.getDescontoFidelidade(), politica.getQuantidadeOsFidelidade(), concluidas, canceladas,
                request.pisoMinimo(), politica.getAjusteMinimo(), politica.getAjusteMaximo());

        SugestaoPreco sugestao = sugestoes.save(new SugestaoPreco(ordem.getId(), ordem.getCliente().getId(),
                execucao == null ? null : execucao.getId(), calculo, responsavel));
        return SugestaoResponse.de(sugestao);
    }

    @Transactional
    SugestaoResponse aprovar(Long sugestaoId, String email) {
        SugestaoPreco sugestao = sugestoes.findById(sugestaoId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Sugestão de preço não encontrada."));
        OrdemServico ordem = ordens.buscarParaAtualizacao(sugestao.getOrdemServicoId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Ordem de serviço não encontrada."));
        if (ordem.getStatusAtual() != StatusOrdemServico.RESERVADA)
            throw new RegraNegocioException("O preço só pode ser aprovado enquanto a ordem estiver reservada.");
        sugestao.aprovar(buscarResponsavel(email));
        if (ordem.getValorTotal().compareTo(sugestao.getPrecoBase()) != 0)
            throw new RegraNegocioException("O preço da ordem mudou. Consulte uma nova sugestão antes de confirmar.");
        ordem.alterarValorTotal(sugestao.getPrecoSugerido());
        return SugestaoResponse.de(sugestao);
    }

    private Usuario buscarResponsavel(String email) {
        return usuarios.findByEmail(email).orElseThrow(() ->
                new RecursoNaoEncontradoException("Usuário responsável não encontrado."));
    }
}
