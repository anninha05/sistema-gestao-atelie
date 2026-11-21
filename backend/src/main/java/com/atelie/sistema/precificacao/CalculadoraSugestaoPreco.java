package com.atelie.sistema.precificacao;

import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraSugestaoPreco {

    public record Calculo(BigDecimal precoBase, double ajustePid, BigDecimal precoAjustado,
            BigDecimal descontoFidelidade, BigDecimal precoAposFidelidade, BigDecimal pisoMinimo,
            BigDecimal precoSugerido, int ordensConcluidas, int ordensCanceladas,
            boolean elegivelFidelidade, String justificativa) {
    }

    public Calculo calcular(BigDecimal precoBase, double ajustePid, BigDecimal descontoFidelidade,
            Integer minimoOrdensFidelidade, int ordensConcluidas, int ordensCanceladas, BigDecimal pisoMinimo) {
        return calcular(precoBase, ajustePid, descontoFidelidade, minimoOrdensFidelidade,
                ordensConcluidas, ordensCanceladas, pisoMinimo, null, null);
    }

    public Calculo calcular(BigDecimal precoBase, double ajustePid, BigDecimal descontoFidelidade,
            Integer minimoOrdensFidelidade, int ordensConcluidas, int ordensCanceladas, BigDecimal pisoMinimo,
            Double ajusteMinimo, Double ajusteMaximo) {
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
        String explicacaoLimites = "";
        if (ajusteMinimo != null || ajusteMaximo != null) {
            if (ajusteMinimo == null || ajusteMaximo == null || !Double.isFinite(ajusteMinimo)
                    || !Double.isFinite(ajusteMaximo) || ajusteMinimo <= -1 || ajusteMinimo >= ajusteMaximo)
                throw new RegraNegocioException("Os limites da sugestão de preço são inválidos.");
            BigDecimal menor = normalizar(base.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(ajusteMinimo))));
            BigDecimal maior = normalizar(base.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(ajusteMaximo))));
            if (piso.compareTo(maior) > 0)
                throw new RegraNegocioException("O preço mínimo cadastrado supera o aumento permitido sobre o preço atual. Revise o preço-base ou o mínimo da ordem.");
            sugerido = sugerido.max(menor).min(maior);
            explicacaoLimites = " Faixa permitida sobre o preço-base: " + percentual(ajusteMinimo)
                    + " a " + percentual(ajusteMaximo) + "; o preço final respeita essa faixa e o piso.";
        }
        String justificativa = "Preço-base " + moeda(base) + "; ajuste PID de "
                + percentual(ajustePid) + " resulta em " + moeda(precoAjustado) + ". "
                + (elegivel ? "Desconto de fidelidade de " + percentual(descontoAplicado.doubleValue())
                    + " aplicado após " + ordensConcluidas + " ordens concluídas. "
                    : "Sem desconto de fidelidade: " + ordensConcluidas + " ordens concluídas. ")
                + "Piso informado pela gerência: " + moeda(piso) + "; preço sugerido: " + moeda(sugerido) + "."
                + (ordensCanceladas > 0 ? " Histórico contém " + ordensCanceladas
                    + " ordem(ns) cancelada(s); avalie o sinal conforme a política do ateliê." : "");
        return new Calculo(base, ajustePid, precoAjustado, descontoAplicado.setScale(4, RoundingMode.HALF_UP),
                aposFidelidade, piso, sugerido, ordensConcluidas, ordensCanceladas, elegivel, justificativa);
    }

    private BigDecimal normalizar(BigDecimal valor) { return valor.setScale(2, RoundingMode.HALF_UP); }

    private String moeda(BigDecimal valor) { return "R$ " + valor.toPlainString(); }

    private String percentual(double valor) {
        return BigDecimal.valueOf(valor * 100).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
