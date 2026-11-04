package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.cliente.Cliente;
import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrdemServicoStateTest {

    private OrdemServico os;

    @BeforeEach
    void setUp() {
        Cliente cliente = new Cliente("Joana", "22988888888", "joana@email.com", "98765432100");
        os = new OrdemServico(cliente, TipoFluxo.LOCACAO, new BigDecimal("350.00"), LocalDateTime.now().plusDays(5));
    }

    @Test
    @DisplayName("Nova O.S. deve iniciar no estado RESERVADA")
    void deveIniciarReservada() {
        assertEquals(StatusOrdemServico.RESERVADA, os.getStatusAtual());
        assertInstanceOf(Reservada.class, os.getEstado());
    }

    @Test
    @DisplayName("Deve transicionar de RESERVADA para EM_PROVA")
    void deveAvancarParaProva() {
        os.enviarParaProva();
        assertEquals(StatusOrdemServico.EM_PROVA, os.getStatusAtual());
        assertInstanceOf(EmProva.class, os.getEstado());
    }

    @Test
    @DisplayName("Não deve permitir retirar roupa diretamente do estado RESERVADA")
    void naoDeveRetirarDiretoDeReservada() {
        assertThrows(RegraNegocioException.class, () -> os.retirar());
    }

    @Test
    @DisplayName("Deve seguir fluxo completo: Reservada -> EmProva -> Passadoria -> Concluido -> Retirada -> Devolvida")
    void fluxoCompletoDeEstados() {
        os.enviarParaProva();
        assertEquals(StatusOrdemServico.EM_PROVA, os.getStatusAtual());

        assertThrows(RegraNegocioException.class, os::retirar);
        os.iniciarPassadoria();
        os.concluir();
        assertEquals(StatusOrdemServico.CONCLUIDO, os.getStatusAtual());
        os.retirar();
        assertEquals(StatusOrdemServico.RETIRADA, os.getStatusAtual());
        assertInstanceOf(Retirada.class, os.getEstado());

        os.devolver();
        assertEquals(StatusOrdemServico.DEVOLVIDA, os.getStatusAtual());
        assertInstanceOf(Devolvida.class, os.getEstado());
    }

    @Test
    @DisplayName("Não deve permitir devolver roupa que ainda está em prova")
    void naoDeveDevolverEmProva() {
        os.enviarParaProva();
        assertThrows(RegraNegocioException.class, () -> os.devolver());
    }
}
