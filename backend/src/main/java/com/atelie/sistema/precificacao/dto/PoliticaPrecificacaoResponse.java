package com.atelie.sistema.precificacao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PoliticaPrecificacaoResponse(double ganhoProporcional, double ganhoIntegral, double ganhoDerivativo,
        double ajusteMinimo, double ajusteMaximo, BigDecimal descontoFidelidade, Integer quantidadeOsFidelidade,
        Long responsavelId, String responsavelNome, LocalDateTime atualizadaEm) {}
