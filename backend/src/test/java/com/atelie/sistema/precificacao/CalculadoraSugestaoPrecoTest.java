package com.atelie.sistema.precificacao;

import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraSugestaoPrecoTest {
    private final CalculadoraSugestaoPreco calculadora = new CalculadoraSugestaoPreco();

    @Test
    void limitaReducaoTotalIncluindoFidelidadeERejeitaMinimoAcimaDoTeto() {
        var calculadora = new CalculadoraSugestaoPreco();
        var resultado = calculadora.calcular(new BigDecimal("150"), -0.2, new BigDecimal("0.1"),
                1, 3, 0, new BigDecimal("100"), -0.2, 0.2);
        assertEquals(new BigDecimal("120.00"), resultado.precoSugerido());
        assertThrows(RegraNegocioException.class, () -> calculadora.calcular(new BigDecimal("150"),
                0.1, null, null, 0, 0, new BigDecimal("200"), -0.2, 0.2));
    }

    @Test
    void aplicaPidFidelidadeEPisoNaOrdemDefinida() {
        var calculo = calculadora.calcular(new BigDecimal("100.00"), 0.10, new BigDecimal("0.10"),
                3, 4, 1, new BigDecimal("105.00"));

        assertEquals(new BigDecimal("110.00"), calculo.precoAjustado());
        assertEquals(new BigDecimal("0.1000"), calculo.descontoFidelidade());
        assertEquals(new BigDecimal("99.00"), calculo.precoAposFidelidade());
        assertEquals(new BigDecimal("105.00"), calculo.precoSugerido());
        assertTrue(calculo.elegivelFidelidade());
        assertTrue(calculo.justificativa().contains("avalie o sinal"));
    }

    @Test
    void naoAplicaFidelidadeAntesDoNumeroConfiguradoDeOrdens() {
        var calculo = calculadora.calcular(new BigDecimal("100.00"), 0, new BigDecimal("0.10"),
                3, 2, 0, new BigDecimal("80.00"));

        assertFalse(calculo.elegivelFidelidade());
        assertEquals(BigDecimal.ZERO.setScale(4), calculo.descontoFidelidade());
        assertEquals(new BigDecimal("100.00"), calculo.precoSugerido());
    }

    @Test
    void limiteInferiorProtegePrecoMesmoAposDescontoDeFidelidade() {
        var calculo = calculadora.calcular(new BigDecimal("80.00"), -0.05, new BigDecimal("0.10"),
                2, 5, 0, new BigDecimal("75.00"));

        assertEquals(new BigDecimal("76.00"), calculo.precoAjustado());
        assertEquals(new BigDecimal("68.40"), calculo.precoAposFidelidade());
        assertEquals(new BigDecimal("75.00"), calculo.precoSugerido());
    }

    @Test
    void rejeitaPrecoBasePisoOuResultadoInvalidos() {
        assertThrows(RegraNegocioException.class, () -> calculadora.calcular(
                BigDecimal.ZERO, 0, null, null, 0, 0, BigDecimal.ONE));
        assertThrows(RegraNegocioException.class, () -> calculadora.calcular(
                new BigDecimal("100"), 0, null, null, 0, 0, BigDecimal.ZERO));
        assertThrows(RegraNegocioException.class, () -> calculadora.calcular(
                new BigDecimal("100"), -2, null, null, 0, 0, BigDecimal.ONE));
        assertThrows(RegraNegocioException.class, () -> calculadora.calcular(
                new BigDecimal("100"), Double.NaN, null, null, 0, 0, BigDecimal.ONE));
    }
}
