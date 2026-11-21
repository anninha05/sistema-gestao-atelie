package com.atelie.sistema.precificacao;

import com.atelie.sistema.shared.exception.RegraNegocioException;
import com.atelie.sistema.usuario.Perfil;
import com.atelie.sistema.usuario.Usuario;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ControladorPIDTest {
    private final ControladorPID controlador = new ControladorPID();
    private final Usuario gerente = new Usuario("Gerente", "gerente@atelie.com", "senha", Perfil.ROLE_ADMIN);

    @Test
    void erroZeroParteSemAjuste() {
        var resultado = controlador.calcular(0, 1, null, 0, politica(0.4, 0.02, 0.1, -1, 1));

        assertEquals(0, resultado.termoProporcional());
        assertEquals(0, resultado.termoIntegral());
        assertEquals(0, resultado.termoDerivativo());
        assertEquals(0, resultado.ajusteLimitado());
    }

    @Test
    void normalizaErroPelaReferenciaAcumuladaDaMeta() {
        var meta = new BigDecimal("10000.00");

        assertEquals(0.1, controlador.normalizarErro(meta, new BigDecimal("4000.00"), 10, 20), 1e-12);
        assertEquals(-0.1, controlador.normalizarErro(meta, new BigDecimal("6000.00"), 10, 20), 1e-12);
        assertEquals(0, controlador.normalizarErro(meta, new BigDecimal("5000.00"), 10, 20), 1e-12);
    }

    @Test
    void normalizacaoRejeitaMetaZeradaEDataForaDaCompetencia() {
        assertThrows(RegraNegocioException.class, () -> controlador.normalizarErro(
                BigDecimal.ZERO, BigDecimal.ZERO, 1, 30));
        assertThrows(RegraNegocioException.class, () -> controlador.normalizarErro(
                BigDecimal.ONE, BigDecimal.ZERO, 31, 30));
        assertThrows(RegraNegocioException.class, () -> controlador.normalizarErro(
                BigDecimal.ONE, BigDecimal.ZERO, 0, 30));
    }

    @Test
    void errosPositivoENegativoGeramReacoesDeSinalEsperado() {
        var politica = politica(0.5, 0, 0, -1, 1);

        assertTrue(controlador.calcular(0.2, 1, null, 0, politica).ajusteLimitado() > 0);
        assertTrue(controlador.calcular(-0.2, 1, null, 0, politica).ajusteLimitado() < 0);
    }

    @Test
    void primeiraAmostraZeraDerivativoEIntervaloPosteriorUsaErroAnterior() {
        var politica = politica(0, 0, 0.4, -1, 1);

        assertEquals(0, controlador.calcular(0.3, 1, null, 0, politica).termoDerivativo());
        assertEquals(0.04, controlador.calcular(0.3, 2, 0.1, 0, politica).termoDerivativo(), 1e-12);
    }

    @Test
    void acumulaIntegralPeloErroEIntervaloEmDias() {
        var resultado = controlador.calcular(0.1, 2, 0.1, 0.2, politica(0, 0.5, 0, -1, 1));

        assertEquals(0.4, resultado.integralAcumulada(), 1e-12);
        assertEquals(0.2, resultado.termoIntegral(), 1e-12);
    }

    @Test
    void limitaSaidaNosDoisLimitesEEvitaAcumuloContraSaturacao() {
        var politica = politica(1, 0.5, 0, -0.05, 0.05);
        var acima = controlador.calcular(0.5, 1, null, 0, politica);
        var abaixo = controlador.calcular(-0.5, 1, null, 0, politica);

        assertEquals(0.05, acima.ajusteLimitado(), 1e-12);
        assertEquals(-0.05, abaixo.ajusteLimitado(), 1e-12);
        assertEquals(0, acima.integralAcumulada(), 1e-12);
        assertEquals(0, abaixo.integralAcumulada(), 1e-12);
    }

    @Test
    void rejeitaErroIntervaloEstadoOuResultadoInvalidos() {
        var politica = politica(1, 0, 0, -1, 1);
        assertThrows(RegraNegocioException.class, () -> controlador.calcular(Double.NaN, 1, null, 0, politica));
        assertThrows(RegraNegocioException.class, () -> controlador.calcular(0.1, 0, null, 0, politica));
        assertThrows(RegraNegocioException.class, () -> controlador.calcular(0.1, -1, null, 0, politica));
        assertThrows(RegraNegocioException.class, () -> controlador.calcular(0.1, 1, Double.POSITIVE_INFINITY, 0, politica));
        assertThrows(RegraNegocioException.class, () -> controlador.calcular(Double.MAX_VALUE, 2, null, 0, politica));
    }

    private ControladorPID.Ganhos politica(double kp, double ki, double kd, double minimo, double maximo) {
        return new PoliticaPrecificacao(kp, ki, kd, minimo, maximo, BigDecimal.ZERO, 1, gerente).getGanhos();
    }
}
