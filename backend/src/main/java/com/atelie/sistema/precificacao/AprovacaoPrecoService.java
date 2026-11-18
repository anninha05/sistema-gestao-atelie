package com.atelie.sistema.precificacao;

import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.ordemservico.OrdemServicoRepository;
import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.precificacao.dto.AprovarPrecoRequest;
import com.atelie.sistema.precificacao.dto.AprovacaoPrecoResponse;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import com.atelie.sistema.usuario.Usuario;
import com.atelie.sistema.usuario.UsuarioRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AprovacaoPrecoService {
    private final OrdemServicoRepository ordens;
    private final SugestaoPrecoRepository sugestoes;
    private final AprovacaoPrecoRepository aprovacoes;
    private final UsuarioRepository usuarios;

    public AprovacaoPrecoService(OrdemServicoRepository ordens, SugestaoPrecoRepository sugestoes,
            AprovacaoPrecoRepository aprovacoes, UsuarioRepository usuarios) {
        this.ordens = ordens;
        this.sugestoes = sugestoes;
        this.aprovacoes = aprovacoes;
        this.usuarios = usuarios;
    }

    @Transactional
    public AprovacaoPrecoResponse aprovar(UUID ordemId, AprovarPrecoRequest request, String email) {
        if (request == null || request.sugestaoId() == null)
            throw new RegraNegocioException("Informe a sugestão de preço que deseja aprovar.");
        OrdemServico ordem = ordens.buscarParaAtualizacao(ordemId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Ordem de serviço não encontrada."));
        if (ordem.getStatusAtual() != StatusOrdemServico.RESERVADA)
            throw new RegraNegocioException("O preço só pode ser aprovado enquanto a ordem estiver reservada.");
        SugestaoPreco sugestao = sugestoes.findById(request.sugestaoId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Sugestão de preço não encontrada."));
        if (!ordem.getId().equals(sugestao.getOrdemServicoId()))
            throw new RegraNegocioException("A sugestão informada pertence a outra ordem de serviço.");
        if (aprovacoes.existsBySugestaoId(sugestao.getId()))
            throw new RegraNegocioException("Esta sugestão já foi aprovada.");

        Usuario responsavel = usuarios.findByEmail(email).orElseThrow(() ->
                new RecursoNaoEncontradoException("Usuário responsável não encontrado."));
        var pagamento = ordem.getPagamento();
        var anterior = ordem.getValorTotal();
        var recebido = pagamento.getValorPago();
        ordem.alterarValorTotal(sugestao.getPrecoSugerido());
        AprovacaoPreco aprovacao = aprovacoes.save(new AprovacaoPreco(ordem.getId(), sugestao.getId(), anterior,
                ordem.getValorTotal(), recebido, responsavel));
        return new AprovacaoPrecoResponse(aprovacao.getId(), aprovacao.getOrdemServicoId(),
                aprovacao.getSugestaoId(), aprovacao.getValorAnterior(), aprovacao.getValorAprovado(),
                aprovacao.getValorRecebido(), responsavel.getId(), responsavel.getNome(), aprovacao.getAprovadaEm());
    }
}
