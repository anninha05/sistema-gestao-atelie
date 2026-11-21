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
    @DisplayName("Retirada de locação transiciona de DISPONIVEL para ALUGADA")
    void deveAlugarPecaDisponivel() {
        peca.retirarParaLocacao();
        assertEquals(StatusDisponibilidade.ALUGADA, peca.getStatusDisponibilidade());
        assertInstanceOf(Alugada.class, peca.getEstado());
    }

    @Test
    @DisplayName("Não deve permitir alugar uma peça que já está alugada")
    void naoDeveAlugarPecaJaAlugada() {
        peca.retirarParaLocacao();
        assertThrows(RegraNegocioException.class, () -> peca.retirarParaLocacao());
    }

    @Test
    @DisplayName("Deve enviar peça para lavagem e depois devolver ao estoque")
    void fluxoLavagemEDevolucao() {
        peca.retirarParaLocacao();
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

        assertThrows(RegraNegocioException.class, () -> peca.retirarParaLocacao());
    }

    @Test
    void naoDeveEnviarPecaAlugadaParaLavagemSemDevolucaoDaOrdem() {
        peca.retirarParaLocacao();
        assertThrows(RegraNegocioException.class, peca::enviarParaLavagem);
        assertEquals(StatusDisponibilidade.ALUGADA, peca.getStatusDisponibilidade());
    }

    @Test
    void deveTratarManutencaoLegadaComoCostura() {
        carregarComoGravado(peca, StatusDisponibilidade.MANUTENCAO);
        assertInstanceOf(EmCostura.class, peca.getEstado());
        assertThrows(RegraNegocioException.class, peca::retirarParaLocacao);
    }

    @Test
    void naoDeveLiberarPecaAlugadaDiretamente() {
        peca.retirarParaLocacao();
        assertThrows(RegraNegocioException.class, () -> peca.devolverParaEstoque(true, true));
        assertEquals(StatusDisponibilidade.ALUGADA, peca.getStatusDisponibilidade());
    }

    @Test
    void deveExigirLavagemEConferenciaParaLiberar() {
        peca.retirarParaLocacao();
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
        peca.retirarParaLocacao();
        peca.devolverDaLocacao();
        peca.enviarParaCostura();
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
        assertThrows(RegraNegocioException.class, modelo::retirarParaLocacao);
        carregarComoGravado(modelo, StatusDisponibilidade.ALUGADA);
        assertThrows(RegraNegocioException.class, modelo::devolverDaLocacao);
    }

    private static void carregarComoGravado(Peca alvo, StatusDisponibilidade status) {
        try {
            var coluna = Peca.class.getDeclaredField("statusDisponibilidade");
            coluna.setAccessible(true);
            coluna.set(alvo, status);
            var aposCarregar = Peca.class.getDeclaredMethod("onPostLoad");
            aposCarregar.setAccessible(true);
            aposCarregar.invoke(alvo);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
