package com.atelie.sistema.ordemservico;

import com.atelie.sistema.cliente.Cliente;
import com.atelie.sistema.cliente.ClienteService;
import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.peca.PecaRepository;
import com.atelie.sistema.peca.TipoFluxo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardCompromissosTest {
    @Test
    void listaCompromissosDoDiaEmOrdemEExcluiOrdensCanceladas() {
        var dia = LocalDate.of(2026, 12, 15);
        var inicio = dia.atTime(9, 0);
        var cliente = new Cliente("Ana", "22999999999", "ana@email.com", "52998224725");
        var prova = new OrdemServico(cliente, TipoFluxo.SOB_MEDIDA, new BigDecimal("300"), inicio.plusDays(5));
        prova.setEstado(new com.atelie.sistema.ordemservico.state.EtapaProducao(StatusOrdemServico.CONFECCAO));
        // A data de prova é modelada na OS; para teste puro, o agendamento usa a API de reagendamento.
        prova.reagendar(null, null, inicio.plusHours(2), null, inicio.plusDays(5));

        var cancelada = new OrdemServico(cliente, TipoFluxo.SOB_MEDIDA, new BigDecimal("300"), inicio);
        cancelada.cancelar();
        var repository = mock(OrdemServicoRepository.class);
        when(repository.findAll()).thenReturn(List.of(prova, cancelada));
        var service = new OrdemServicoService(repository, mock(ClienteService.class), mock(PecaRepository.class));

        var compromissos = service.compromissosDoDia(dia);
        assertEquals(1, compromissos.size());
        assertEquals("Prova", compromissos.getFirst().tipoCompromisso());
        assertEquals(inicio.plusHours(2), compromissos.getFirst().dataHora());
    }
}
