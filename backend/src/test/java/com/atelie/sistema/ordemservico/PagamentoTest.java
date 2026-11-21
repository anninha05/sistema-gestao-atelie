package com.atelie.sistema.ordemservico;

import com.atelie.sistema.cliente.Cliente;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PagamentoTest {

    private OrdemServico os;
    private Pagamento pagamento;

    @BeforeEach
    void setUp() {
        Cliente cliente = new Cliente("Ana", "22999999999", "ana@email.com", "12345678901");
        os = new OrdemServico(cliente, TipoFluxo.LOCACAO, new BigDecimal("500.00"), LocalDateTime.now().plusDays(7));
        pagamento = os.getPagamento();
    }

    @Test
    @DisplayName("Deve calcular saldo = total - pago após sinal e após pagamentos parciais")
    void deveCalcularSaldoAposSinalEPagamentosParciais() {
        assertEquals(new BigDecimal("500.00"), pagamento.getValorTotal());
        assertEquals(new BigDecimal("0.00"), pagamento.getValorSinalPago());
        assertEquals(new BigDecimal("0.00"), pagamento.getValorPago());
        assertEquals(new BigDecimal("500.00"), pagamento.getSaldoDevedor());

        pagamento.registrarRecebimento(new BigDecimal("150.00"), LocalDate.of(2026, 10, 28), MetodoPagamento.PIX, true);
        assertEquals(new BigDecimal("150.00"), pagamento.getValorSinalPago());
        assertEquals(new BigDecimal("150.00"), pagamento.getValorPago());
        assertEquals(new BigDecimal("350.00"), pagamento.getSaldoDevedor());
        assertEquals(LocalDate.of(2026, 10, 28), pagamento.getDataPagamentoSinal());

        pagamento.registrarRecebimento(new BigDecimal("100.00"), LocalDate.now(), MetodoPagamento.PIX, false);
        assertEquals(new BigDecimal("150.00"), pagamento.getValorSinalPago());
        assertEquals(new BigDecimal("250.00"), pagamento.getValorPago());
        assertEquals(new BigDecimal("250.00"), pagamento.getSaldoDevedor());
    }

    @Test
    @DisplayName("Deve transicionar status financeiro: PENDENTE -> SINAL_PAGO -> QUITADO")
    void deveTransicionarStatusFinanceiro() {
        assertEquals(StatusFinanceiro.PENDENTE, pagamento.getStatusFinanceiro());

        os.registrarRecebimento(new BigDecimal("200.00"), LocalDate.now(), MetodoPagamento.PIX, true);
        assertEquals(StatusFinanceiro.SINAL_PAGO, pagamento.getStatusFinanceiro());

        os.registrarRecebimento(new BigDecimal("300.00"), LocalDate.now(), MetodoPagamento.PIX, false);
        assertEquals(StatusFinanceiro.QUITADO, pagamento.getStatusFinanceiro());
        assertEquals(new BigDecimal("0.00"), pagamento.getSaldoDevedor());
    }

    @Test
    @DisplayName("Deve rejeitar valores menores ou iguais a zero no construtor, sinal, pagamento e alteração de total")
    void deveRejeitarValorMenorOuIgualAZero() {
        assertThrows(RegraNegocioException.class, () -> new Pagamento(os, BigDecimal.ZERO));
        assertThrows(RegraNegocioException.class, () -> new Pagamento(os, new BigDecimal("-10.00")));

        assertThrows(RegraNegocioException.class, () -> pagamento.registrarRecebimento(BigDecimal.ZERO, LocalDate.now(), MetodoPagamento.PIX, true));
        assertThrows(RegraNegocioException.class, () -> pagamento.registrarRecebimento(new BigDecimal("-50.00"), LocalDate.now(), MetodoPagamento.PIX, true));

        assertThrows(RegraNegocioException.class, () -> pagamento.registrarRecebimento(BigDecimal.ZERO, LocalDate.now(), MetodoPagamento.PIX, false));
        assertThrows(RegraNegocioException.class, () -> pagamento.registrarRecebimento(new BigDecimal("-1.00"), LocalDate.now(), MetodoPagamento.PIX, false));

        assertThrows(RegraNegocioException.class, () -> pagamento.alterarValorTotal(BigDecimal.ZERO));
        assertThrows(RegraNegocioException.class, () -> pagamento.alterarValorTotal(new BigDecimal("-100.00")));
    }

    @Test
    @DisplayName("Deve rejeitar pagamento ou sinal maior que o saldo devedor")
    void deveRejeitarPagamentoMaiorQueSaldo() {
        assertThrows(RegraNegocioException.class,
                () -> pagamento.registrarRecebimento(new BigDecimal("500.01"), LocalDate.now(), MetodoPagamento.PIX, true));

        pagamento.registrarRecebimento(new BigDecimal("200.00"), LocalDate.now(), MetodoPagamento.PIX, true);

        assertThrows(RegraNegocioException.class,
                () -> pagamento.registrarRecebimento(new BigDecimal("300.01"), LocalDate.now(), MetodoPagamento.PIX, false));
    }

    @Test
    @DisplayName("Deve rejeitar registro de segundo sinal")
    void deveRejeitarSegundoSinal() {
        pagamento.registrarRecebimento(new BigDecimal("100.00"), LocalDate.now(), MetodoPagamento.PIX, true);

        assertThrows(RegraNegocioException.class,
                () -> pagamento.registrarRecebimento(new BigDecimal("50.00"), LocalDate.now(), MetodoPagamento.PIX, true));
    }

    @Test
    @DisplayName("Deve rejeitar reduzir valorTotal abaixo do valor já pago e permitir quando >= valorPago")
    void deveRejeitarReduzirValorTotalAbaixoDoPago() {
        pagamento.registrarRecebimento(new BigDecimal("200.00"), LocalDate.now(), MetodoPagamento.PIX, true);
        pagamento.registrarRecebimento(new BigDecimal("100.00"), LocalDate.now(), MetodoPagamento.PIX, false);

        assertThrows(RegraNegocioException.class,
                () -> pagamento.alterarValorTotal(new BigDecimal("299.99")));

        pagamento.alterarValorTotal(new BigDecimal("300.00"));
        assertEquals(new BigDecimal("300.00"), pagamento.getValorTotal());
        assertEquals(new BigDecimal("0.00"), pagamento.getSaldoDevedor());
        assertEquals(StatusFinanceiro.QUITADO, pagamento.getStatusFinanceiro());
    }

    @Test
    @DisplayName("Todo lançamento entra no histórico de recebimentos, inclusive o sinal")
    void deveRegistrarCadaLancamentoNoHistorico() {
        pagamento.registrarRecebimento(new BigDecimal("150.00"), LocalDate.of(2026, 11, 2), MetodoPagamento.PIX, true);
        pagamento.registrarRecebimento(new BigDecimal("50.00"), LocalDate.of(2026, 11, 9), MetodoPagamento.DINHEIRO, false);

        assertEquals(2, pagamento.getRecebimentos().size());
        assertTrue(pagamento.getRecebimentos().getFirst().isSinal());
        assertEquals(new BigDecimal("200.00"), pagamento.getRecebimentos().stream()
                .map(Recebimento::getValor).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(new BigDecimal("200.00"), pagamento.getValorPago());
    }

    @Test
    void deveRejeitarRecebimentoSemDataOuMetodoSemAlterarSaldo() {
        assertThrows(RegraNegocioException.class,
                () -> pagamento.registrarRecebimento(new BigDecimal("10.00"), null, MetodoPagamento.PIX, false));
        assertThrows(RegraNegocioException.class,
                () -> pagamento.registrarRecebimento(new BigDecimal("10.00"), LocalDate.now(), null, false));
        assertEquals(new BigDecimal("500.00"), pagamento.getSaldoDevedor());
        assertTrue(pagamento.getRecebimentos().isEmpty());
    }

    @Test
    @DisplayName("Precisão decimal: 100.10 + 99.90 não deve gerar erro de arredondamento")
    void deveGarantirPrecisaoDecimalSemErroDeArredondamento() {
        Pagamento pagPrecisao = new Pagamento(os, new BigDecimal("200.00"));

        pagPrecisao.registrarRecebimento(new BigDecimal("100.10"), LocalDate.now(), MetodoPagamento.PIX, true);
        assertEquals(new BigDecimal("99.90"), pagPrecisao.getSaldoDevedor());
        assertEquals(StatusFinanceiro.SINAL_PAGO, pagPrecisao.getStatusFinanceiro());

        pagPrecisao.registrarRecebimento(new BigDecimal("99.90"), LocalDate.now(), MetodoPagamento.PIX, false);
        assertEquals(new BigDecimal("200.00"), pagPrecisao.getValorPago());
        assertEquals(new BigDecimal("0.00"), pagPrecisao.getSaldoDevedor());
        assertEquals(StatusFinanceiro.QUITADO, pagPrecisao.getStatusFinanceiro());
    }
}

