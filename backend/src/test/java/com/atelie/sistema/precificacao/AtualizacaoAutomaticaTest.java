package com.atelie.sistema.precificacao;

import static org.mockito.Mockito.*;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AtualizacaoAutomaticaTest {
    @Test
    void semMetaNaoCalculaNemCriaPoliticaPorContaPropria() {
        var metas = mock(MetaMensalRepository.class);
        var politicas = mock(PoliticaPrecificacaoRepository.class);
        when(metas.buscarPorMes(YearMonth.now().atDay(1))).thenReturn(Optional.empty());
        var servico = new AtualizacaoDiariaPIDService(metas, null, null, politicas, null, null, null, null);
        servico.atualizarAutomaticamente();
        verifyNoInteractions(politicas);
    }

    @Test
    void semCalibracaoNaoExecutaControlador() {
        var metas = mock(MetaMensalRepository.class);
        var politicas = mock(PoliticaPrecificacaoRepository.class);
        var controlador = mock(ControladorPID.class);
        when(metas.buscarPorMes(YearMonth.now().atDay(1))).thenReturn(Optional.of(mock(MetaMensal.class)));
        var servico = new AtualizacaoDiariaPIDService(metas, null, null, politicas, null, null, null, controlador);
        servico.atualizarAutomaticamente();
        verifyNoInteractions(controlador);
        verify(politicas, never()).save(any());
    }
}
