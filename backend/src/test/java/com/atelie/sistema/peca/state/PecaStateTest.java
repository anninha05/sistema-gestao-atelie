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
        peca.devolverDaLocacao();
        assertEquals(StatusDisponibilidade.EM_LAVAGEM, peca.getStatusDisponibilidade());
        assertInstanceOf(EmLavagem.class, peca.getEstado());

        peca.devolverParaEstoque(true, true);
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

    @Test
    void naoDeveLiberarPecaAlugadaDiretamente() {
        peca.marcarComoAlugada();
        assertThrows(RegraNegocioException.class, () -> peca.devolverParaEstoque(true, true));
        assertEquals(StatusDisponibilidade.ALUGADA, peca.getStatusDisponibilidade());
    }

    @Test
    void deveExigirLavagemEConferenciaParaLiberar() {
        peca.marcarComoAlugada();
        peca.devolverDaLocacao();
        assertThrows(RegraNegocioException.class, () -> peca.devolverParaEstoque(false, true));
        assertThrows(RegraNegocioException.class, () -> peca.devolverParaEstoque(true, false));
        assertEquals(StatusDisponibilidade.EM_LAVAGEM, peca.getStatusDisponibilidade());
        assertThrows(RegraNegocioException.class, peca::retirarParaLocacao);
        peca.devolverParaEstoque(true, true);
        assertEquals(StatusDisponibilidade.DISPONIVEL, peca.getStatusDisponibilidade());
    }

    @Test
    void deveManterPecaIndisponivelDuranteReparos() {
        peca.marcarComoAlugada();
        peca.devolverDaLocacao();
        peca.enviarParaManutencao();
        assertEquals(StatusDisponibilidade.EM_COSTURA, peca.getStatusDisponibilidade());
        assertThrows(RegraNegocioException.class, peca::retirarParaLocacao);
        assertThrows(RegraNegocioException.class, () -> peca.devolverParaEstoque(true, false));
        peca.enviarParaLavagem();
        peca.devolverParaEstoque(true, true);
        assertEquals(StatusDisponibilidade.DISPONIVEL, peca.getStatusDisponibilidade());
    }

    @Test
    void naoDeveDevolverPecaDisponivelOuSobMedida() {
        assertThrows(RegraNegocioException.class, peca::devolverDaLocacao);
        var modelo = new Peca("Modelo sob medida", TipoFluxo.SOB_MEDIDA);
        modelo.setEstado(new Alugada());
        assertThrows(RegraNegocioException.class, modelo::devolverDaLocacao);
    }
}
