package com.atelie.sistema.precificacao;

import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.ordemservico.OrdemServicoRepository;
import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.precificacao.dto.AprovarPrecoRequest;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import com.atelie.sistema.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AprovacaoPrecoTest {
    @Test
    void rejeitaSugestaoQuandoPrecoDaOrdemMudou() {
        verificarRejeicao("160.00", Optional.empty());
    }

    @Test
    void rejeitaSugestaoQuandoMinimoCadastradoFoiAumentado() {
        var piso = mock(PisoPrecoOrdem.class);
        when(piso.getValor()).thenReturn(new BigDecimal("145.00"));
        verificarRejeicao("150.00", Optional.of(piso));
    }

    private void verificarRejeicao(String precoAtual, Optional<PisoPrecoOrdem> piso) {
        var ordens = mock(OrdemServicoRepository.class);
        var sugestoes = mock(SugestaoPrecoRepository.class);
        var aprovacoes = mock(AprovacaoPrecoRepository.class);
        var pisos = mock(PisoPrecoOrdemRepository.class);
        var ordem = mock(OrdemServico.class);
        var sugestao = mock(SugestaoPreco.class);
        var id = UUID.randomUUID();
        when(ordens.buscarParaAtualizacao(id)).thenReturn(Optional.of(ordem));
        when(ordem.getId()).thenReturn(id);
        when(ordem.getStatusAtual()).thenReturn(StatusOrdemServico.RESERVADA);
        when(ordem.getValorTotal()).thenReturn(new BigDecimal(precoAtual));
        when(sugestoes.findById(1L)).thenReturn(Optional.of(sugestao));
        when(sugestao.getId()).thenReturn(1L);
        when(sugestao.getOrdemServicoId()).thenReturn(id);
        when(sugestao.getPrecoBase()).thenReturn(new BigDecimal("150.00"));
        when(sugestao.getPrecoSugerido()).thenReturn(new BigDecimal("140.00"));
        when(pisos.findById(id)).thenReturn(piso);
        var servico = new AprovacaoPrecoService(ordens, sugestoes, aprovacoes,
                mock(UsuarioRepository.class), pisos);
        assertThrows(RegraNegocioException.class, () ->
                servico.aprovar(id, new AprovarPrecoRequest(1L), "gerente@atelie.com"));
        verify(ordem, never()).alterarValorTotal(any());
        verify(aprovacoes, never()).save(any());
    }
}
