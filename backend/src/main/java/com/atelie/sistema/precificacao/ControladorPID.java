package com.atelie.sistema.precificacao;

import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Implementação manual do PID discreto usado pela precificação do Linho. */
@Component
public class ControladorPID {

    public record Ganhos(double kp, double ki, double kd, double ajusteMinimo, double ajusteMaximo) {
        public Ganhos {
            if (!Double.isFinite(kp) || !Double.isFinite(ki) || !Double.isFinite(kd)
                    || !Double.isFinite(ajusteMinimo) || !Double.isFinite(ajusteMaximo) || ajusteMinimo >= ajusteMaximo)
                throw new RegraNegocioException("Informe ganhos PID finitos e limites de ajuste válidos.");
        }

        public Ganhos comGanhos(double novoKp, double novoKi, double novoKd) {
            return new Ganhos(novoKp, novoKi, novoKd, ajusteMinimo, ajusteMaximo);
        }
    }

    public record Resultado(double erroNormalizado, double termoProporcional, double termoIntegral,
            double termoDerivativo, double saidaBruta, double ajusteLimitado, double integralAcumulada) {
    }

    public double normalizarErro(BigDecimal metaMensal, BigDecimal resultadoRealizado,
            int diaAtual, int diasNoMes) {
        if (metaMensal == null || resultadoRealizado == null || metaMensal.signum() <= 0)
            throw new RegraNegocioException("Informe uma meta positiva e um resultado realizado.");
        if (diasNoMes < 1 || diaAtual < 1 || diaAtual > diasNoMes)
            throw new RegraNegocioException("Informe um dia válido dentro da competência.");
        double meta = metaMensal.doubleValue();
        double realizado = resultadoRealizado.doubleValue();
        if (!Double.isFinite(meta) || !Double.isFinite(realizado))
            throw new RegraNegocioException("Meta e resultado realizado devem caber no intervalo numérico permitido.");
        double referenciaAcumulada = meta * diaAtual / diasNoMes;
        double erro = (referenciaAcumulada - realizado) / meta;
        validarFinito(referenciaAcumulada, erro);
        return erro;
    }

    public Resultado calcular(double erroNormalizado, double deltaDias, Double erroAnterior,
            double integralAnterior, Ganhos ganhos) {
        if (ganhos == null) throw new RegraNegocioException("Informe os ganhos do PID antes do cálculo.");
        if (!Double.isFinite(erroNormalizado) || !Double.isFinite(deltaDias)
                || !Double.isFinite(integralAnterior)
                || erroAnterior != null && !Double.isFinite(erroAnterior))
            throw new RegraNegocioException("O cálculo PID exige valores numéricos finitos.");
        if (deltaDias <= 0) throw new RegraNegocioException("O intervalo entre cálculos deve ser maior que zero.");

        double proporcional = ganhos.kp() * erroNormalizado;
        double derivativo = erroAnterior == null ? 0
                : ganhos.kd() * (erroNormalizado - erroAnterior) / deltaDias;
        double integralCandidata = integralAnterior + erroNormalizado * deltaDias;
        validarFinito(proporcional, derivativo, integralCandidata);

        double integral = integralCandidata;
        double saidaBruta = proporcional + ganhos.ki() * integral + derivativo;
        validarFinito(saidaBruta);
        double limitada = limitar(saidaBruta, ganhos);
        double contribuicaoNovaIntegral = ganhos.ki() * erroNormalizado * deltaDias;
        boolean integralEmpurraSaturacaoSuperior = saidaBruta > ganhos.ajusteMaximo()
                && contribuicaoNovaIntegral > 0;
        boolean integralEmpurraSaturacaoInferior = saidaBruta < ganhos.ajusteMinimo()
                && contribuicaoNovaIntegral < 0;

        if (integralEmpurraSaturacaoSuperior || integralEmpurraSaturacaoInferior) {
            integral = integralAnterior;
            saidaBruta = proporcional + ganhos.ki() * integral + derivativo;
            validarFinito(saidaBruta);
            limitada = limitar(saidaBruta, ganhos);
        }

        double termoIntegral = ganhos.ki() * integral;
        validarFinito(termoIntegral);
        return new Resultado(erroNormalizado, proporcional, termoIntegral, derivativo,
                saidaBruta, limitada, integral);
    }

    private double limitar(double valor, Ganhos ganhos) {
        return Math.max(ganhos.ajusteMinimo(), Math.min(valor, ganhos.ajusteMaximo()));
    }

    private void validarFinito(double... valores) {
        for (double valor : valores) {
            if (!Double.isFinite(valor))
                throw new RegraNegocioException("O resultado do cálculo PID excedeu o intervalo numérico permitido.");
        }
    }
}
