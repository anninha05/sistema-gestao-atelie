package com.atelie.sistema.precificacao;

import com.atelie.sistema.usuario.Perfil;
import com.atelie.sistema.usuario.Usuario;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetaMensalTest {
    private final Usuario responsavel = new Usuario("Gerente", "gerente@atelie.com", "senha", Perfil.ROLE_ADMIN);

    @Test
    void guardaCompetenciaIndicadorValorEResponsavel() {
        var meta = new MetaMensal(YearMonth.of(2026, 11), new BigDecimal("12000"),
                IndicadorPrecificacao.RESULTADO_DE_CAIXA, responsavel);

        assertEquals(YearMonth.of(2026, 11), meta.getMes());
        assertEquals(new BigDecimal("12000.00"), meta.getValorMeta());
        assertEquals(IndicadorPrecificacao.RESULTADO_DE_CAIXA, meta.getIndicador());
        assertSame(responsavel, meta.getResponsavel());
    }

    @Test
    void rejeitaMetaNulaOuNaoPositiva() {
        assertThrows(RegraNegocioException.class, () -> new MetaMensal(YearMonth.of(2026, 11),
                BigDecimal.ZERO, IndicadorPrecificacao.RESULTADO_DE_CAIXA, responsavel));
        assertThrows(RegraNegocioException.class, () -> new MetaMensal(YearMonth.of(2026, 11),
                null, IndicadorPrecificacao.RESULTADO_DE_CAIXA, responsavel));
    }
}
