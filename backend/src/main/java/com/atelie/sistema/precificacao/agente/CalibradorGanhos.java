package com.atelie.sistema.precificacao.agente;

import com.atelie.sistema.precificacao.ControladorPID;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class CalibradorGanhos {

    public record Resultado(ControladorPID.Ganhos inicial, double custoInicial, ControladorPID.Ganhos calibrado,
            double custoFinal, List<Double> historicoCusto) {
    }

    private static final double[] PASSO_MINIMO = {0.1, 0.01, 0.05};

    private final double tolerancia;
    private final int maximoIteracoes;

    public CalibradorGanhos(double tolerancia, int maximoIteracoes) {
        this.tolerancia = tolerancia;
        this.maximoIteracoes = maximoIteracoes;
    }

    public Resultado calibrar(ControladorPID.Ganhos inicial, ToDoubleFunction<ControladorPID.Ganhos> custo) {
        double[] ganhos = {inicial.kp(), inicial.ki(), inicial.kd()};
        double[] passos = new double[3];
        for (int i = 0; i < 3; i++) {
            passos[i] = Math.max(PASSO_MINIMO[i], ganhos[i] / 2);
        }
        double custoInicial = custo.applyAsDouble(inicial);
        double melhor = custoInicial;
        List<Double> historico = new ArrayList<>(List.of(melhor));

        for (int iteracao = 0; iteracao < maximoIteracoes && soma(passos) > tolerancia; iteracao++) {
            for (int i = 0; i < 3; i++) {
                double original = ganhos[i];
                ganhos[i] = original + passos[i];
                double candidato = custo.applyAsDouble(montar(inicial, ganhos));
                if (candidato < melhor) {
                    melhor = candidato;
                    passos[i] *= 1.1;
                    continue;
                }
                ganhos[i] = Math.max(0, original - passos[i]);
                candidato = custo.applyAsDouble(montar(inicial, ganhos));
                if (candidato < melhor) {
                    melhor = candidato;
                    passos[i] *= 1.1;
                    continue;
                }
                ganhos[i] = original;
                passos[i] *= 0.9;
            }
            historico.add(melhor);
        }
        return new Resultado(inicial, custoInicial, montar(inicial, ganhos), melhor, List.copyOf(historico));
    }

    private ControladorPID.Ganhos montar(ControladorPID.Ganhos base, double[] ganhos) {
        return base.comGanhos(ganhos[0], ganhos[1], ganhos[2]);
    }

    private double soma(double[] valores) {
        double total = 0;
        for (double valor : valores) {
            total += valor;
        }
        return total;
    }
}
