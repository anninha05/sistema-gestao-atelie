package com.atelie.sistema.ordemservico.dto;

import com.atelie.sistema.ordemservico.MetodoPagamento;
import com.atelie.sistema.ordemservico.StatusFinanceiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PagamentoResponse(
        BigDecimal valorTotal,
        BigDecimal valorSinalPago,
        BigDecimal valorPago,
        BigDecimal saldoDevedor,
        StatusFinanceiro statusFinanceiro,
        List<Movimento> recebimentos) {

    public record Movimento(BigDecimal valor, LocalDate data, MetodoPagamento metodo, boolean sinal) {
    }
}
