package com.atelie.sistema.ordemservico;

import com.atelie.sistema.cliente.Cliente;
import com.atelie.sistema.cliente.ClienteService;
import com.atelie.sistema.ordemservico.dto.ReagendarOrdemServicoRequest;
import com.atelie.sistema.peca.Peca;
import com.atelie.sistema.peca.PecaRepository;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReagendamentoTest {
    @Mock OrdemServicoRepository ordens;
    @Mock ClienteService clientes;
    @Mock PecaRepository pecas;

    @Test
    void reagendaReservaSemContarAPropriaOrdemComoConflito() {
        UUID ordemId = UUID.randomUUID();
        UUID pecaId = UUID.randomUUID();
        var inicioOriginal = LocalDateTime.of(2026, 12, 1, 10, 0);
        var fimOriginal = inicioOriginal.plusDays(1);
        OrdemServico ordem = ordem(pecaId, inicioOriginal, fimOriginal);
        Peca peca = new Peca("Vestido", TipoFluxo.LOCACAO);
        when(ordens.buscarParaAtualizacao(ordemId)).thenReturn(Optional.of(ordem));
        when(pecas.buscarParaReserva(pecaId)).thenReturn(Optional.of(peca));
        when(ordens.contarReservasDeOutrasOrdens(any(), any(), any(), eq(ordemId))).thenReturn(0L);

        var inicioNovo = LocalDateTime.of(2027, 1, 5, 10, 0);
        var fimNovo = inicioNovo.plusDays(2);
        new OrdemServicoService(ordens, clientes, pecas).reagendar(ordemId,
                new ReagendarOrdemServicoRequest(inicioNovo, fimNovo, null, null, null));

        assertEquals(inicioNovo, ordem.getInicioLocacao());
        assertEquals(fimNovo, ordem.getFimLocacao());
        verify(ordens).contarReservasDeOutrasOrdens(pecaId, inicioNovo, fimNovo, ordemId);
    }

    @Test
    void rejeitaConflitoSemAlterarDatasOriginais() {
        UUID ordemId = UUID.randomUUID();
        UUID pecaId = UUID.randomUUID();
        var inicioOriginal = LocalDateTime.of(2026, 12, 1, 10, 0);
        var fimOriginal = inicioOriginal.plusDays(1);
        OrdemServico ordem = ordem(pecaId, inicioOriginal, fimOriginal);
        when(ordens.buscarParaAtualizacao(ordemId)).thenReturn(Optional.of(ordem));
        when(pecas.buscarParaReserva(pecaId)).thenReturn(Optional.of(new Peca("Vestido", TipoFluxo.LOCACAO)));
        when(ordens.contarReservasDeOutrasOrdens(any(), any(), any(), eq(ordemId))).thenReturn(1L);

        var inicioNovo = LocalDateTime.of(2027, 1, 5, 10, 0);
        assertThrows(RegraNegocioException.class, () -> new OrdemServicoService(ordens, clientes, pecas)
                .reagendar(ordemId, new ReagendarOrdemServicoRequest(inicioNovo, inicioNovo.plusDays(1), null, null, null)));

        assertEquals(inicioOriginal, ordem.getInicioLocacao());
        assertEquals(fimOriginal, ordem.getFimLocacao());
    }

    private OrdemServico ordem(UUID pecaId, LocalDateTime inicio, LocalDateTime fim) {
        Cliente cliente = new Cliente("Ana", "22999999999", "ana@email.com", "52998224725");
        return new OrdemServico(cliente, TipoFluxo.LOCACAO, new BigDecimal("100.00"), null,
                List.of(new ItemOrdemServico(pecaId, new BigDecimal("100.00"))), BigDecimal.ZERO,
                inicio, fim, null, null, null, null);
    }
}
