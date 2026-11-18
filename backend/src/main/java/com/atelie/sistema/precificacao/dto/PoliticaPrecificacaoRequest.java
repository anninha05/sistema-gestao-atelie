package com.atelie.sistema.precificacao.dto;

import java.math.BigDecimal;

public record PoliticaPrecificacaoRequest(double ganhoProporcional, double ganhoIntegral, double ganhoDerivativo,
        double ajusteMinimo, double ajusteMaximo, BigDecimal descontoFidelidade, Integer quantidadeOsFidelidade) {}
