package com.atelie.sistema.precificacao;

import java.time.LocalDate;
import java.time.YearMonth;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoPIDTest {
    @Test
    void registraEstadoApenasUmaVezPorDiaEDentroDaCompetencia() {
        var estado = new EstadoPID(YearMonth.of(2026, 11));
        estado.registrar(0.25, null, LocalDate.of(2026, 11, 4));

        assertEquals(0.25, estado.getIntegralAcumulada());
        assertNull(estado.getErroAnterior());
        assertEquals(LocalDate.of(2026, 11, 4), estado.getUltimaAtualizacao());
        assertThrows(RegraNegocioException.class, () ->
                estado.registrar(0.5, 0.1, LocalDate.of(2026, 11, 4)));
        assertThrows(RegraNegocioException.class, () ->
                estado.registrar(0.5, 0.1, LocalDate.of(2026, 12, 1)));
    }

    @Test
    void rejeitaValoresNaoFinitos() {
        var estado = new EstadoPID(YearMonth.of(2026, 11));
        assertThrows(RegraNegocioException.class, () ->
                estado.registrar(Double.NaN, 0.1, LocalDate.of(2026, 11, 1)));
        assertThrows(RegraNegocioException.class, () ->
                estado.registrar(0.1, Double.POSITIVE_INFINITY, LocalDate.of(2026, 11, 1)));
    }
}
