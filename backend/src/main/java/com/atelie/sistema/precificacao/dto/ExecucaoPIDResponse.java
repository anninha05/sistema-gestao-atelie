package com.atelie.sistema.precificacao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record ExecucaoPIDResponse(Long id, YearMonth mes, LocalDate dataApuracao, long deltaDias,
        BigDecimal valorMeta, BigDecimal resultadoRealizado, BigDecimal referenciaAcumulada,
        double erroNormalizado, double termoProporcional, double termoIntegral, double termoDerivativo,
        double saidaBruta, double ajusteLimitado, double integralAcumulada, double ganhoProporcional,
        double ganhoIntegral, double ganhoDerivativo, double ajusteMinimo, double ajusteMaximo,
        Long responsavelId, String responsavelNome) {}
