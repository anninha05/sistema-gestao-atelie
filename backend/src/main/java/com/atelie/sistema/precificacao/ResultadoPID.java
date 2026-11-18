package com.atelie.sistema.precificacao;

public record ResultadoPID(double erroNormalizado, double termoProporcional, double termoIntegral,
        double termoDerivativo, double saidaBruta, double ajusteLimitado, double integralAcumulada) {}
