package com.atelie.sistema.precificacao;

import java.math.BigDecimal;

public record CalculoSugestaoPreco(BigDecimal precoBase, double ajustePid, BigDecimal precoAjustado,
        BigDecimal descontoFidelidade, BigDecimal precoAposFidelidade, BigDecimal pisoMinimo,
        BigDecimal precoSugerido, int ordensConcluidas, int ordensCanceladas,
        boolean elegivelFidelidade, String justificativa) {}
