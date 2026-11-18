package com.atelie.sistema.precificacao;

import com.atelie.sistema.usuario.Perfil;
import com.atelie.sistema.usuario.Usuario;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PoliticaPrecificacaoTest {
    private final Usuario responsavel = new Usuario("Gerente", "gerente@atelie.com", "senha", Perfil.ROLE_ADMIN);

    @Test
    void guardaGanhosLimitesERegraDeFidelidadeConfiguraveis() {
        var politica = new PoliticaPrecificacao(0.4, 0.02, 0.1, -0.05, 0.05,
                new BigDecimal("0.10"), 3, responsavel);

        assertEquals(0.4, politica.getGanhoProporcional());
        assertEquals(-0.05, politica.getAjusteMinimo());
        assertEquals(0.05, politica.getAjusteMaximo());
        assertEquals(new BigDecimal("0.1000"), politica.getDescontoFidelidade());
        assertEquals(3, politica.getQuantidadeOsFidelidade());
    }

    @Test
    void rejeitaLimitesInvertidosERegraDeFidelidadeIncompleta() {
        assertThrows(RegraNegocioException.class, () -> new PoliticaPrecificacao(
                0, 0, 0, 0.05, -0.05, null, null, responsavel));
        assertThrows(RegraNegocioException.class, () -> new PoliticaPrecificacao(
                0, 0, 0, -0.05, 0.05, new BigDecimal("0.10"), null, responsavel));
    }

    @Test
    void rejeitaGanhosNaNOuInfinity() {
        assertThrows(RegraNegocioException.class, () -> new PoliticaPrecificacao(
                Double.NaN, 0, 0, -0.05, 0.05, null, null, responsavel));
        assertThrows(RegraNegocioException.class, () -> new PoliticaPrecificacao(
                0, 0, Double.POSITIVE_INFINITY, -0.05, 0.05, null, null, responsavel));
    }
}
