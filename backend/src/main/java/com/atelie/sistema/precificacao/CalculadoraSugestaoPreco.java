package com.atelie.sistema.precificacao;

import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraSugestaoPreco {
    public CalculoSugestaoPreco calcular(BigDecimal precoBase, double ajustePid, BigDecimal descontoFidelidade,
            Integer minimoOrdensFidelidade, int ordensConcluidas, int ordensCanceladas, BigDecimal pisoMinimo) {
        if (precoBase == null || precoBase.signum() <= 0 || !Double.isFinite(ajustePid)
                || ordensConcluidas < 0 || ordensCanceladas < 0)
            throw new RegraNegocioException("Informe preço-base, ajuste PID e histórico válidos.");
        if (pisoMinimo == null || pisoMinimo.signum() <= 0)
            throw new RegraNegocioException("Informe um piso mínimo positivo definido pela gerência.");
        if ((descontoFidelidade == null) != (minimoOrdensFidelidade == null)
                || descontoFidelidade != null && (descontoFidelidade.signum() < 0
                || descontoFidelidade.compareTo(BigDecimal.ONE) >= 0 || minimoOrdensFidelidade < 1))
            throw new RegraNegocioException("A regra de fidelidade configurada é inválida.");

        BigDecimal base = normalizar(precoBase);
        BigDecimal precoAjustado = base.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(ajustePid)))
                .setScale(2, RoundingMode.HALF_UP);
        if (precoAjustado.signum() <= 0)
            throw new RegraNegocioException("O ajuste PID não pode produzir um preço não positivo.");
        boolean elegivel = descontoFidelidade != null && ordensConcluidas >= minimoOrdensFidelidade;
        BigDecimal descontoAplicado = elegivel ? descontoFidelidade : BigDecimal.ZERO;
        BigDecimal aposFidelidade = precoAjustado.multiply(BigDecimal.ONE.subtract(descontoAplicado))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal piso = normalizar(pisoMinimo);
        BigDecimal sugerido = aposFidelidade.max(piso);
        String justificativa = "Preço-base " + moeda(base) + "; ajuste PID de "
                + percentual(ajustePid) + " resulta em " + moeda(precoAjustado) + ". "
                + (elegivel ? "Desconto de fidelidade de " + percentual(descontoAplicado.doubleValue())
                    + " aplicado após " + ordensConcluidas + " ordens concluídas. "
                    : "Sem desconto de fidelidade: " + ordensConcluidas + " ordens concluídas. ")
                + "Piso informado pela gerência: " + moeda(piso) + "; preço sugerido: " + moeda(sugerido) + "."
                + (ordensCanceladas > 0 ? " Histórico contém " + ordensCanceladas
                    + " ordem(ns) cancelada(s); avalie o sinal conforme a política do ateliê." : "");
        return new CalculoSugestaoPreco(base, ajustePid, precoAjustado, descontoAplicado.setScale(4, RoundingMode.HALF_UP),
                aposFidelidade, piso, sugerido, ordensConcluidas, ordensCanceladas, elegivel, justificativa);
    }

    private BigDecimal normalizar(BigDecimal valor) { return valor.setScale(2, RoundingMode.HALF_UP); }

    private String moeda(BigDecimal valor) { return "R$ " + valor.toPlainString(); }

    private String percentual(double valor) {
        return BigDecimal.valueOf(valor * 100).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
