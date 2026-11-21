package com.atelie.sistema.precificacao.agente;

import com.atelie.sistema.precificacao.ControladorPID;
import java.math.BigDecimal;

public class AgentePrecificacao {

    public record Percepcao(BigDecimal meta, BigDecimal realizado, int dia, int diasNoMes, double deltaDias) {
    }

    private final ControladorPID controlador;
    private final ControladorPID.Ganhos ganhos;
    private double integral;
    private Double erroAnterior;

    public AgentePrecificacao(ControladorPID controlador, ControladorPID.Ganhos ganhos) {
        this(controlador, ganhos, 0, null);
    }

    public AgentePrecificacao(ControladorPID controlador, ControladorPID.Ganhos ganhos, double integral,
            Double erroAnterior) {
        this.controlador = controlador;
        this.ganhos = ganhos;
        this.integral = integral;
        this.erroAnterior = erroAnterior;
    }

    public ControladorPID.Resultado agir(Percepcao percepcao) {
        double erro = controlador.normalizarErro(percepcao.meta(), percepcao.realizado(), percepcao.dia(),
                percepcao.diasNoMes());
        ControladorPID.Resultado resultado = controlador.calcular(erro, percepcao.deltaDias(), erroAnterior, integral, ganhos);
        integral = resultado.integralAcumulada();
        erroAnterior = erro;
        return resultado;
    }

    public ControladorPID.Ganhos getGanhos() {
        return ganhos;
    }

    public double getIntegral() {
        return integral;
    }

    public Double getErroAnterior() {
        return erroAnterior;
    }
}
