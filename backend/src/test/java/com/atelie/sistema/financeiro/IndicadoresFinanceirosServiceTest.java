package com.atelie.sistema.financeiro;

import com.atelie.sistema.ordemservico.PagamentoRepository;
import com.atelie.sistema.ordemservico.RecebimentoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IndicadoresFinanceirosServiceTest {
    @Test
    void somaSomenteFluxosRealizadosNoMesEExplicitaResultadoDeCaixa() {
        var recebimentos = mock(RecebimentoRepository.class);
        var despesas = mock(DespesaRepository.class);
        var pagamentos = mock(PagamentoRepository.class);
        var inicio = LocalDate.of(2026, 11, 1);
        var fim = LocalDate.of(2026, 12, 1);
        when(recebimentos.somarRecebimentos(inicio, fim)).thenReturn(new BigDecimal("600.00"));
        when(despesas.somarPagamentos(inicio, fim)).thenReturn(new BigDecimal("200.00"));
        when(pagamentos.somarSaldoEmAberto()).thenReturn(new BigDecimal("150.00"));

        var resposta = new IndicadoresFinanceirosService(recebimentos, despesas, pagamentos)
                .consultar(YearMonth.of(2026, 11));

        assertEquals(new BigDecimal("600.00"), resposta.recebimentosRealizados());
        assertEquals(new BigDecimal("200.00"), resposta.despesasPagas());
        assertEquals(new BigDecimal("400.00"), resposta.resultadoDeCaixa());
        assertEquals(new BigDecimal("150.00"), resposta.saldoTotalAReceber());
        assertTrue(resposta.observacao().contains("Não representa lucro"));
        verify(recebimentos).somarRecebimentos(inicio, fim);
        verify(despesas).somarPagamentos(inicio, fim);
    }
}
