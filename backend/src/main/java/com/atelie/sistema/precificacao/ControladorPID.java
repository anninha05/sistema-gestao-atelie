package com.atelie.sistema.precificacao;

import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.math.BigDecimal;

/** Implementação manual do PID discreto usado pela precificação do Linho. */
public class ControladorPID {
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

    public ResultadoPID calcular(double erroNormalizado, double deltaDias, Double erroAnterior,
            double integralAnterior, PoliticaPrecificacao politica) {
        if (politica == null) throw new RegraNegocioException("Configure a política PID antes do cálculo.");
        if (!Double.isFinite(erroNormalizado) || !Double.isFinite(deltaDias)
                || !Double.isFinite(integralAnterior)
                || erroAnterior != null && !Double.isFinite(erroAnterior))
            throw new RegraNegocioException("O cálculo PID exige valores numéricos finitos.");
        if (deltaDias <= 0) throw new RegraNegocioException("O intervalo entre cálculos deve ser maior que zero.");

        double proporcional = politica.getGanhoProporcional() * erroNormalizado;
        double derivativo = erroAnterior == null ? 0
                : politica.getGanhoDerivativo() * (erroNormalizado - erroAnterior) / deltaDias;
        double integralCandidata = integralAnterior + erroNormalizado * deltaDias;
        validarFinito(proporcional, derivativo, integralCandidata);

        double integral = integralCandidata;
        double saidaBruta = proporcional + politica.getGanhoIntegral() * integral + derivativo;
        validarFinito(saidaBruta);
        double limitada = limitar(saidaBruta, politica);
        double contribuicaoNovaIntegral = politica.getGanhoIntegral() * erroNormalizado * deltaDias;
        boolean integralEmpurraSaturacaoSuperior = saidaBruta > politica.getAjusteMaximo()
                && contribuicaoNovaIntegral > 0;
        boolean integralEmpurraSaturacaoInferior = saidaBruta < politica.getAjusteMinimo()
                && contribuicaoNovaIntegral < 0;

        if (integralEmpurraSaturacaoSuperior || integralEmpurraSaturacaoInferior) {
            integral = integralAnterior;
            saidaBruta = proporcional + politica.getGanhoIntegral() * integral + derivativo;
            validarFinito(saidaBruta);
            limitada = limitar(saidaBruta, politica);
        }

        double termoIntegral = politica.getGanhoIntegral() * integral;
        validarFinito(termoIntegral);
        return new ResultadoPID(erroNormalizado, proporcional, termoIntegral, derivativo,
                saidaBruta, limitada, integral);
    }

    private double limitar(double valor, PoliticaPrecificacao politica) {
        return Math.max(politica.getAjusteMinimo(), Math.min(valor, politica.getAjusteMaximo()));
    }

    private void validarFinito(double... valores) {
        for (double valor : valores) {
            if (!Double.isFinite(valor))
                throw new RegraNegocioException("O resultado do cálculo PID excedeu o intervalo numérico permitido.");
        }
    }
}
