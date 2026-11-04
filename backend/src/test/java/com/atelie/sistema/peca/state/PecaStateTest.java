package com.atelie.sistema.peca.state;

import com.atelie.sistema.peca.Peca;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PecaStateTest {

    private Peca peca;

    @BeforeEach
    void setUp() {
        peca = new Peca("Vestido de Gala", TipoFluxo.LOCACAO);
    }

    @Test
    @DisplayName("Peça recém-criada deve iniciar com estado DISPONIVEL")
    void deveIniciarDisponivel() {
        assertEquals(StatusDisponibilidade.DISPONIVEL, peca.getStatusDisponibilidade());
        assertInstanceOf(Disponivel.class, peca.getEstado());
    }

    @Test
    @DisplayName("Deve transicionar de DISPONIVEL para ALUGADA com sucesso")
    void deveAlugarPecaDisponivel() {
        peca.marcarComoAlugada();
        assertEquals(StatusDisponibilidade.ALUGADA, peca.getStatusDisponibilidade());
        assertInstanceOf(Alugada.class, peca.getEstado());
    }

    @Test
    @DisplayName("Não deve permitir alugar uma peça que já está alugada")
    void naoDeveAlugarPecaJaAlugada() {
        peca.marcarComoAlugada();
        assertThrows(RegraNegocioException.class, () -> peca.marcarComoAlugada());
    }

    @Test
    @DisplayName("Deve enviar peça para lavagem e depois devolver ao estoque")
    void fluxoLavagemEDevolucao() {
        peca.marcarComoAlugada();
        peca.enviarParaLavagem();
        assertEquals(StatusDisponibilidade.EM_LAVAGEM, peca.getStatusDisponibilidade());
        assertInstanceOf(EmLavagem.class, peca.getEstado());

        peca.devolverParaEstoque();
        assertEquals(StatusDisponibilidade.DISPONIVEL, peca.getStatusDisponibilidade());
        assertInstanceOf(Disponivel.class, peca.getEstado());
    }

    @Test
    @DisplayName("Deve enviar peça para costura a partir de disponível")
    void deveEnviarParaCostura() {
        peca.enviarParaCostura();
        assertEquals(StatusDisponibilidade.EM_COSTURA, peca.getStatusDisponibilidade());
        assertInstanceOf(EmCostura.class, peca.getEstado());

        assertThrows(RegraNegocioException.class, () -> peca.marcarComoAlugada());
    }
}
