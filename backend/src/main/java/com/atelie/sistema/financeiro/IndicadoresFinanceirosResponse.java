package com.atelie.sistema.financeiro;

import java.math.BigDecimal;
import java.time.YearMonth;

public record IndicadoresFinanceirosResponse(YearMonth mes, BigDecimal recebimentosRealizados,
        BigDecimal despesasPagas, BigDecimal resultadoDeCaixa, BigDecimal saldoTotalAReceber,
        String observacao) {}
