package com.atelie.sistema.precificacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.atelie.sistema.shared.exception.RegraNegocioException;
import com.atelie.sistema.usuario.Perfil;
import com.atelie.sistema.usuario.Usuario;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrecificacaoDominioTest {

    private final Usuario gerente = new Usuario("Gerente", "gerente@atelie.com", "senha", Perfil.ROLE_ADMIN);

    @Test
    void metaGuardaCompetenciaValorEResponsavel() {
        var meta = new MetaMensal(YearMonth.of(2026, 11), new BigDecimal("12000"), gerente);

        assertEquals(YearMonth.of(2026, 11), meta.getMes());
        assertEquals(new BigDecimal("12000.00"), meta.getValorMeta());
        assertSame(gerente, meta.getResponsavel());
    }

    @Test
    void metaRejeitaValorNuloOuNaoPositivo() {
        assertThrows(RegraNegocioException.class, () -> new MetaMensal(YearMonth.of(2026, 11), BigDecimal.ZERO, gerente));
        assertThrows(RegraNegocioException.class, () -> new MetaMensal(YearMonth.of(2026, 11), null, gerente));
    }

    @Test
    void politicaGuardaGanhosLimitesEFidelidade() {
        var politica = new PoliticaPrecificacao(0.4, 0.02, 0.1, -0.05, 0.05, new BigDecimal("0.10"), 3, gerente);

        assertEquals(new ControladorPID.Ganhos(0.4, 0.02, 0.1, -0.05, 0.05), politica.getGanhos());
        assertEquals(new BigDecimal("0.1000"), politica.getDescontoFidelidade());
        assertEquals(Integer.valueOf(3), politica.getQuantidadeOsFidelidade());
    }

    @Test
    void politicaRejeitaLimitesInvertidosGanhosInvalidosEFidelidadeIncompleta() {
        assertThrows(RegraNegocioException.class, () -> new PoliticaPrecificacao(0, 0, 0, 0.05, -0.05, null, null, gerente));
        assertThrows(RegraNegocioException.class, () -> new PoliticaPrecificacao(Double.NaN, 0, 0, -0.05, 0.05, null, null, gerente));
        assertThrows(RegraNegocioException.class,
                () -> new PoliticaPrecificacao(0, 0, 0, -0.05, 0.05, new BigDecimal("0.10"), null, gerente));
    }

    @Test
    void sugestaoSoPodeSerAprovadaUmaVez() {
        var calculo = new CalculadoraSugestaoPreco().calcular(new BigDecimal("150.00"), 0.1, null, null, 0, 0,
                new BigDecimal("100.00"));
        var sugestao = new SugestaoPreco(UUID.randomUUID(), UUID.randomUUID(), null, calculo, gerente);

        sugestao.aprovar(gerente);
        assertSame(gerente, sugestao.getAprovadaPor());
        assertNotNull(sugestao.getAprovadaEm());
        assertThrows(RegraNegocioException.class, () -> sugestao.aprovar(gerente));
    }

    @Test
    void semMetaOuSemPoliticaOAgenteNaoAtua() {
        var metas = mock(MetaMensalRepository.class);
        var politicas = mock(PoliticaPrecificacaoRepository.class);
        var execucoes = mock(ExecucaoPIDDiariaRepository.class);
        var controlador = mock(ControladorPID.class);
        var servico = new PrecificacaoService(metas, politicas, execucoes, null, null, null, controlador, null, false);

        when(metas.bloquearPorMes(YearMonth.now().atDay(1))).thenReturn(Optional.empty());
        when(politicas.findById(PoliticaPrecificacao.ID_UNICA)).thenReturn(Optional.empty());
        servico.atualizarHoje();

        when(metas.bloquearPorMes(YearMonth.now().atDay(1))).thenReturn(Optional.of(mock(MetaMensal.class)));
        servico.atualizarHoje();

        verifyNoInteractions(controlador, execucoes);
        verify(politicas, never()).save(any());
    }
}
