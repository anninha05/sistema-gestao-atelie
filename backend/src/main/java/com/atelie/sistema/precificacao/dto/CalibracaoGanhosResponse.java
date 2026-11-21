package com.atelie.sistema.precificacao.dto;

public record CalibracaoGanhosResponse(double kpAnterior, double kiAnterior, double kdAnterior, double custoAnterior,
        double kp, double ki, double kd, double custo, int iteracoes) {
}
