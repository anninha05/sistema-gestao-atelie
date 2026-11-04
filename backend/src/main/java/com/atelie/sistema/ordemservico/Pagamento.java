package com.atelie.sistema.ordemservico;

import com.atelie.sistema.shared.exception.RegraNegocioException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "pagamentos")
public class Pagamento {

    private static final int ESCALA = 2;
    private static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_UP;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(ESCALA, ARREDONDAMENTO);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false, unique = true)
    private OrdemServico ordemServico;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "valor_sinal_pago", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorSinalPago = ZERO;

    @Column(name = "valor_pago", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorPago = ZERO;

    @Column(name = "saldo_devedor", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoDevedor;

    @Column(name = "data_pagamento_sinal")
    private LocalDate dataPagamentoSinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_financeiro", nullable = false, length = 50)
    private StatusFinanceiro statusFinanceiro;

    @Version
    private long versao;

    @OneToMany(mappedBy = "pagamento", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private java.util.List<Recebimento> recebimentos = new java.util.ArrayList<>();

    public java.util.List<Recebimento> getRecebimentos() {
        return java.util.List.copyOf(recebimentos);
    }

    public void registrarRecebimento(BigDecimal valor, LocalDate data, MetodoPagamento metodo, boolean sinal) {
        if (data == null || metodo == null) {
            throw new RegraNegocioException("Informe a data e o método de pagamento.");
        }
        if (sinal) {
            registrarSinal(valor, data);
        } else {
            registrarPagamento(valor);
        }
        recebimentos.add(new Recebimento(this, normalizar(valor), data, metodo, sinal));
    }

    protected Pagamento() {
    }

    public Pagamento(OrdemServico os, BigDecimal valorTotal) {
        if (os == null) {
            throw new RegraNegocioException("A ordem de serviço é obrigatória para criar o pagamento.");
        }
        validarValorPositivo(valorTotal, "O valor total do pagamento deve ser maior que zero.");

        this.ordemServico = os;
        this.valorTotal = normalizar(valorTotal);
        this.valorSinalPago = ZERO;
        this.valorPago = ZERO;
        recalcular();
    }

    public void registrarSinal(BigDecimal valor, LocalDate data) {
        if (this.valorSinalPago.compareTo(BigDecimal.ZERO) > 0 || this.dataPagamentoSinal != null) {
            throw new RegraNegocioException("O sinal já foi registrado para esta ordem de serviço.");
        }
        validarValorPositivo(valor, "O valor do sinal deve ser maior que zero.");

        BigDecimal valorNormalizado = normalizar(valor);
        if (valorNormalizado.compareTo(this.saldoDevedor) > 0) {
            throw new RegraNegocioException("O valor do sinal não pode ser maior que o saldo devedor.");
        }

        this.valorSinalPago = valorNormalizado;
        this.valorPago = normalizar(this.valorPago.add(valorNormalizado));
        this.dataPagamentoSinal = data != null ? data : LocalDate.now();
        recalcular();
    }

    public void registrarPagamento(BigDecimal valor) {
        validarValorPositivo(valor, "O valor do pagamento deve ser maior que zero.");

        BigDecimal valorNormalizado = normalizar(valor);
        if (valorNormalizado.compareTo(this.saldoDevedor) > 0) {
            throw new RegraNegocioException("O valor do pagamento não pode ser maior que o saldo devedor.");
        }

        this.valorPago = normalizar(this.valorPago.add(valorNormalizado));
        recalcular();
    }

    public void alterarValorTotal(BigDecimal novo) {
        validarValorPositivo(novo, "O novo valor total deve ser maior que zero.");

        BigDecimal novoNormalizado = normalizar(novo);
        if (novoNormalizado.compareTo(this.valorPago) < 0) {
            throw new RegraNegocioException("O valor total não pode ser menor que o valor já pago.");
        }

        this.valorTotal = novoNormalizado;
        recalcular();
    }

    private void recalcular() {
        this.saldoDevedor = normalizar(this.valorTotal.subtract(this.valorPago));

        if (this.valorPago.compareTo(BigDecimal.ZERO) == 0) {
            this.statusFinanceiro = StatusFinanceiro.PENDENTE;
        } else if (this.valorPago.compareTo(this.valorTotal) < 0) {
            this.statusFinanceiro = StatusFinanceiro.SINAL_PAGO;
        } else {
            this.statusFinanceiro = StatusFinanceiro.QUITADO;
        }
    }

    private void validarValorPositivo(BigDecimal valor, String mensagemErro) {
        if (valor == null || normalizar(valor).signum() <= 0) {
            throw new RegraNegocioException(mensagemErro);
        }
    }

    private BigDecimal normalizar(BigDecimal valor) {
        return valor.setScale(ESCALA, ARREDONDAMENTO);
    }

    public Long getId() {
        return id;
    }

    public OrdemServico getOrdemServico() {
        return ordemServico;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public BigDecimal getValorSinalPago() {
        return valorSinalPago;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public BigDecimal getSaldoDevedor() {
        return saldoDevedor;
    }

    public LocalDate getDataPagamentoSinal() {
        return dataPagamentoSinal;
    }

    public StatusFinanceiro getStatusFinanceiro() {
        return statusFinanceiro;
    }
}

