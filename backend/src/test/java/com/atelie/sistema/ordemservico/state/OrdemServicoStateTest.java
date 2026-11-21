package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.cliente.Cliente;
import com.atelie.sistema.ordemservico.MetodoPagamento;
import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        quitar(os);
        assertThrows(RegraNegocioException.class, () -> os.retirar());
    }

    @Test
    @DisplayName("Deve seguir fluxo completo: Reservada -> EmProva -> Passadoria -> Concluido -> Retirada -> Devolvida")
    void fluxoCompletoDeEstados() {
        os.enviarParaProva();
        assertEquals(StatusOrdemServico.EM_PROVA, os.getStatusAtual());

        assertThrows(RegraNegocioException.class, os::retirar);
        os.iniciarPassadoria();
        assertInstanceOf(EmPassadoria.class, os.getEstado());
        os.concluir();
        assertEquals(StatusOrdemServico.CONCLUIDO, os.getStatusAtual());
        assertInstanceOf(Concluida.class, os.getEstado());
        quitar(os);
        os.retirar();
        assertEquals(StatusOrdemServico.RETIRADA, os.getStatusAtual());
        assertInstanceOf(Retirada.class, os.getEstado());

        os.devolver();
        assertEquals(StatusOrdemServico.DEVOLVIDA, os.getStatusAtual());
        assertInstanceOf(Devolvida.class, os.getEstado());
        assertThrows(RegraNegocioException.class, os::devolver);
    }

    @Test
    void sobMedidaDeveSerConfeccionadaEEntregueSemDevolucao() {
        var encomenda = new OrdemServico(os.getCliente(), TipoFluxo.SOB_MEDIDA, new BigDecimal("900.00"), null);
        encomenda.iniciarConfeccao();
        assertInstanceOf(EmConfeccao.class, encomenda.getEstado());
        encomenda.enviarParaProva();
        encomenda.iniciarConfeccao();
        assertInstanceOf(EmAjustes.class, encomenda.getEstado());
        encomenda.iniciarPassadoria();
        encomenda.concluir();
        quitar(encomenda);
        encomenda.retirar();
        assertEquals(StatusOrdemServico.ENTREGUE, encomenda.getStatusAtual());
        assertInstanceOf(Entregue.class, encomenda.getEstado());
        assertThrows(RegraNegocioException.class, encomenda::devolver);
    }

    @Test
    void locacaoDeveComecarPelosAjustesAoIniciarTrabalho() {
        os.iniciarConfeccao();
        assertInstanceOf(EmAjustes.class, os.getEstado());
    }

    @Test
    @DisplayName("Não deve permitir devolver roupa que ainda está em prova")
    void naoDeveDevolverEmProva() {
        os.enviarParaProva();
        assertThrows(RegraNegocioException.class, () -> os.devolver());
    }

    @Test
    void deveExigirQuitacaoAntesDaRetiradaSemAlterarEstado() {
        os.enviarParaProva();
        os.iniciarPassadoria();
        os.concluir();
        os.registrarRecebimento(new BigDecimal("100.00"), LocalDate.now(), MetodoPagamento.PIX, true);
        var erro = assertThrows(RegraNegocioException.class, os::retirar);
        assertEquals("Quite o saldo devedor da ordem antes de registrar a retirada ou entrega.", erro.getMessage());
        assertEquals(StatusOrdemServico.CONCLUIDO, os.getStatusAtual());
    }

    @Test
    void cancelamentoDevePreservarRecebimentosERegistrarPendencia() {
        os.registrarRecebimento(new BigDecimal("100.00"), LocalDate.now(), MetodoPagamento.PIX, true);
        os.cancelar();
        assertInstanceOf(Cancelada.class, os.getEstado());
        assertTrue(os.isPendenciaAcerto());
        assertEquals(1, os.getPagamento().getRecebimentos().size());
        assertThrows(RegraNegocioException.class, os::cancelar);
        assertThrows(RegraNegocioException.class,
                () -> os.registrarRecebimento(BigDecimal.TEN, LocalDate.now(), MetodoPagamento.PIX, false));
        assertThrows(RegraNegocioException.class,
                () -> os.reagendar(null, null, LocalDateTime.now(), null, null));
    }

    @Test
    void naoDeveCancelarOuReagendarOrdemRetirada() {
        os.enviarParaProva();
        os.iniciarPassadoria();
        os.concluir();
        quitar(os);
        os.retirar();
        assertThrows(RegraNegocioException.class, os::cancelar);
        assertThrows(RegraNegocioException.class,
                () -> os.reagendar(LocalDateTime.now(), LocalDateTime.now().plusDays(1), null, null, null));
        assertEquals(StatusOrdemServico.RETIRADA, os.getStatusAtual());
    }

    @Test
    void naoDeveDevolverSobMedidaMesmoComEstadoLegadoRetirada() {
        var encomenda = new OrdemServico(os.getCliente(), TipoFluxo.SOB_MEDIDA, new BigDecimal("350.00"), null);
        carregarComoGravado(encomenda, StatusOrdemServico.RETIRADA);
        assertThrows(RegraNegocioException.class, encomenda::devolver);
        assertEquals(StatusOrdemServico.RETIRADA, encomenda.getStatusAtual());
    }

    @Test
    void valoresLegadosDevemSerLidosComoEstadosAtuais() {
        carregarComoGravado(os, StatusOrdemServico.PENDENTE);
        assertInstanceOf(Reservada.class, os.getEstado());
        carregarComoGravado(os, StatusOrdemServico.PROVA);
        assertInstanceOf(EmProva.class, os.getEstado());
        os.iniciarPassadoria();
        assertEquals(StatusOrdemServico.PASSADORIA, os.getStatusAtual());
    }

    private static void quitar(OrdemServico ordem) {
        ordem.registrarRecebimento(ordem.getPagamento().getSaldoDevedor(), LocalDate.now(), MetodoPagamento.PIX, false);
    }

    private static void carregarComoGravado(OrdemServico ordem, StatusOrdemServico status) {
        try {
            var coluna = OrdemServico.class.getDeclaredField("statusAtual");
            coluna.setAccessible(true);
            coluna.set(ordem, status);
            var aposCarregar = OrdemServico.class.getDeclaredMethod("onPostLoad");
            aposCarregar.setAccessible(true);
            aposCarregar.invoke(ordem);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
