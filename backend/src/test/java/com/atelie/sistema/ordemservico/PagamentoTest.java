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

        pagamento.registrarSinal(new BigDecimal("150.00"), LocalDate.of(2026, 10, 28));
        assertEquals(new BigDecimal("150.00"), pagamento.getValorSinalPago());
        assertEquals(new BigDecimal("150.00"), pagamento.getValorPago());
        assertEquals(new BigDecimal("350.00"), pagamento.getSaldoDevedor());
        assertEquals(LocalDate.of(2026, 10, 28), pagamento.getDataPagamentoSinal());

        pagamento.registrarPagamento(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("150.00"), pagamento.getValorSinalPago());
        assertEquals(new BigDecimal("250.00"), pagamento.getValorPago());
        assertEquals(new BigDecimal("250.00"), pagamento.getSaldoDevedor());
    }

    @Test
    @DisplayName("Deve transicionar status financeiro: PENDENTE -> SINAL_PAGO -> QUITADO")
    void deveTransicionarStatusFinanceiro() {
        assertEquals(StatusFinanceiro.PENDENTE, pagamento.getStatusFinanceiro());

        os.registrarSinal(new BigDecimal("200.00"), LocalDate.now());
        assertEquals(StatusFinanceiro.SINAL_PAGO, pagamento.getStatusFinanceiro());

        os.registrarPagamento(new BigDecimal("300.00"));
        assertEquals(StatusFinanceiro.QUITADO, pagamento.getStatusFinanceiro());
        assertEquals(new BigDecimal("0.00"), pagamento.getSaldoDevedor());
    }

    @Test
    @DisplayName("Deve rejeitar valores menores ou iguais a zero no construtor, sinal, pagamento e alteração de total")
    void deveRejeitarValorMenorOuIgualAZero() {
        assertThrows(RegraNegocioException.class, () -> new Pagamento(os, BigDecimal.ZERO));
        assertThrows(RegraNegocioException.class, () -> new Pagamento(os, new BigDecimal("-10.00")));

        assertThrows(RegraNegocioException.class, () -> pagamento.registrarSinal(BigDecimal.ZERO, LocalDate.now()));
        assertThrows(RegraNegocioException.class, () -> pagamento.registrarSinal(new BigDecimal("-50.00"), LocalDate.now()));

        assertThrows(RegraNegocioException.class, () -> pagamento.registrarPagamento(BigDecimal.ZERO));
        assertThrows(RegraNegocioException.class, () -> pagamento.registrarPagamento(new BigDecimal("-1.00")));

        assertThrows(RegraNegocioException.class, () -> pagamento.alterarValorTotal(BigDecimal.ZERO));
        assertThrows(RegraNegocioException.class, () -> pagamento.alterarValorTotal(new BigDecimal("-100.00")));
    }

    @Test
    @DisplayName("Deve rejeitar pagamento ou sinal maior que o saldo devedor")
    void deveRejeitarPagamentoMaiorQueSaldo() {
        assertThrows(RegraNegocioException.class,
                () -> pagamento.registrarSinal(new BigDecimal("500.01"), LocalDate.now()));

        pagamento.registrarSinal(new BigDecimal("200.00"), LocalDate.now());

        assertThrows(RegraNegocioException.class,
                () -> pagamento.registrarPagamento(new BigDecimal("300.01")));
    }

    @Test
    @DisplayName("Deve rejeitar registro de segundo sinal")
    void deveRejeitarSegundoSinal() {
        pagamento.registrarSinal(new BigDecimal("100.00"), LocalDate.now());

        assertThrows(RegraNegocioException.class,
                () -> pagamento.registrarSinal(new BigDecimal("50.00"), LocalDate.now()));
    }

    @Test
    @DisplayName("Deve rejeitar reduzir valorTotal abaixo do valor já pago e permitir quando >= valorPago")
    void deveRejeitarReduzirValorTotalAbaixoDoPago() {
        pagamento.registrarSinal(new BigDecimal("200.00"), LocalDate.now());
        pagamento.registrarPagamento(new BigDecimal("100.00"));

        assertThrows(RegraNegocioException.class,
                () -> pagamento.alterarValorTotal(new BigDecimal("299.99")));

        pagamento.alterarValorTotal(new BigDecimal("300.00"));
        assertEquals(new BigDecimal("300.00"), pagamento.getValorTotal());
        assertEquals(new BigDecimal("0.00"), pagamento.getSaldoDevedor());
        assertEquals(StatusFinanceiro.QUITADO, pagamento.getStatusFinanceiro());
    }

    @Test
    @DisplayName("Precisão decimal: 100.10 + 99.90 não deve gerar erro de arredondamento")
    void deveGarantirPrecisaoDecimalSemErroDeArredondamento() {
        Pagamento pagPrecisao = new Pagamento(os, new BigDecimal("200.00"));

        pagPrecisao.registrarSinal(new BigDecimal("100.10"), LocalDate.now());
        assertEquals(new BigDecimal("99.90"), pagPrecisao.getSaldoDevedor());
        assertEquals(StatusFinanceiro.SINAL_PAGO, pagPrecisao.getStatusFinanceiro());

        pagPrecisao.registrarPagamento(new BigDecimal("99.90"));
        assertEquals(new BigDecimal("200.00"), pagPrecisao.getValorPago());
        assertEquals(new BigDecimal("0.00"), pagPrecisao.getSaldoDevedor());
        assertEquals(StatusFinanceiro.QUITADO, pagPrecisao.getStatusFinanceiro());
    }
}

