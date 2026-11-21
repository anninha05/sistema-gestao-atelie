package com.atelie.sistema.precificacao.agente;

import com.atelie.sistema.precificacao.ControladorPID;
import com.atelie.sistema.precificacao.agente.AmbienteAtelie.Cenario;
import com.atelie.sistema.precificacao.agente.AmbienteAtelie.Episodio;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToDoubleFunction;

public class ExperimentoPrecificacao {

    public static final ControladorPID.Ganhos GANHOS_MANUAIS = new ControladorPID.Ganhos(0.2, 0.01, 0.05, -0.20, 0.20);
    public static final List<Cenario> CENARIOS_DE_TREINO = List.of(Cenario.NORMAL, Cenario.BAIXA_TEMPORADA,
            Cenario.ALTA_TEMPORADA, Cenario.QUEDA_NO_MES);
    private static final int SEMENTES_DE_TREINO = 10;
    private static final int SEMENTES_DE_TESTE = 30;
    private static final long PRIMEIRA_SEMENTE_DE_TESTE = 1000;

    public record Avaliacao(String agente, Cenario cenario, double atingimentoMedio, double atingimentoDesvio,
            double erroMedio, double variacaoPreco, double diasSaturados) {
    }

    private final AmbienteAtelie ambiente;
    private final CalibradorGanhos calibrador;

    public ExperimentoPrecificacao() {
        this(new AmbienteAtelie(AmbienteAtelie.Parametros.PADRAO), new CalibradorGanhos(0.005, 100));
    }

    public ExperimentoPrecificacao(AmbienteAtelie ambiente, CalibradorGanhos calibrador) {
        this.ambiente = ambiente;
        this.calibrador = calibrador;
    }

    public CalibradorGanhos.Resultado treinar(ControladorPID.Ganhos inicial) {
        ToDoubleFunction<ControladorPID.Ganhos> custoMedio = ganhos -> {
            double total = 0;
            for (Cenario cenario : CENARIOS_DE_TREINO) {
                for (long semente = 1; semente <= SEMENTES_DE_TREINO; semente++) {
                    total += ambiente.simular(ganhos, cenario, semente).custo();
                }
            }
            return total / (CENARIOS_DE_TREINO.size() * SEMENTES_DE_TREINO);
        };
        return calibrador.calibrar(inicial, custoMedio);
    }

    public Avaliacao avaliar(String agente, ControladorPID.Ganhos ganhos, Cenario cenario) {
        double[] atingimentos = new double[SEMENTES_DE_TESTE];
        double erro = 0;
        double variacao = 0;
        double saturados = 0;
        for (int i = 0; i < SEMENTES_DE_TESTE; i++) {
            Episodio episodio = ambiente.simular(ganhos, cenario, PRIMEIRA_SEMENTE_DE_TESTE + i);
            atingimentos[i] = episodio.atingimento();
            erro += episodio.erroMedioAbsoluto();
            variacao += episodio.variacaoMediaPreco();
            saturados += episodio.diasSaturados();
        }
        double media = 0;
        for (double valor : atingimentos) {
            media += valor;
        }
        media /= SEMENTES_DE_TESTE;
        double desvio = 0;
        for (double valor : atingimentos) {
            desvio += (valor - media) * (valor - media);
        }
        desvio = Math.sqrt(desvio / (SEMENTES_DE_TESTE - 1));
        return new Avaliacao(agente, cenario, media, desvio, erro / SEMENTES_DE_TESTE, variacao / SEMENTES_DE_TESTE,
                saturados / SEMENTES_DE_TESTE);
    }

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        Path pasta = Path.of(args.length > 0 ? args[0] : "experimentos");
        Files.createDirectories(pasta);
        ExperimentoPrecificacao experimento = new ExperimentoPrecificacao();

        long inicio = System.nanoTime();
        CalibradorGanhos.Resultado treino = experimento.treinar(GANHOS_MANUAIS);
        double segundos = (System.nanoTime() - inicio) / 1e9;

        Map<String, ControladorPID.Ganhos> agentes = new LinkedHashMap<>();
        agentes.put("Sem controle", GANHOS_MANUAIS.comGanhos(0, 0, 0));
        agentes.put("PID manual", GANHOS_MANUAIS);
        agentes.put("PID calibrado", treino.calibrado());

        List<Avaliacao> avaliacoes = new ArrayList<>();
        for (Cenario cenario : Cenario.values()) {
            agentes.forEach((nome, ganhos) -> avaliacoes.add(experimento.avaliar(nome, ganhos, cenario)));
        }

        Files.writeString(pasta.resolve("resultados.csv"), csvResultados(avaliacoes));
        Files.writeString(pasta.resolve("convergencia.csv"), csvConvergencia(treino));
        Files.writeString(pasta.resolve("trajetoria.csv"),
                experimento.csvTrajetoria(agentes, Cenario.BAIXA_TEMPORADA, PRIMEIRA_SEMENTE_DE_TESTE));
        String relatorio = relatorio(treino, segundos, avaliacoes);
        Files.writeString(pasta.resolve("resultados.md"), relatorio);
        System.out.println(relatorio);
    }

    private static String relatorio(CalibradorGanhos.Resultado treino, double segundos, List<Avaliacao> avaliacoes) {
        StringBuilder texto = new StringBuilder("# Resultados do agente de precificação\n\n");
        texto.append("## Treinamento (Twiddle)\n\n")
                .append("| | Kp | Ki | Kd | Custo médio |\n|---|---|---|---|---|\n")
                .append(linhaGanhos("Inicial (manual)", treino.inicial(), treino.custoInicial()))
                .append(linhaGanhos("Calibrado", treino.calibrado(), treino.custoFinal()))
                .append(String.format("%nIterações: %d | Tempo de treinamento: %.2f s | Redução do custo: %.1f%%%n%n",
                        treino.historicoCusto().size() - 1, segundos,
                        100 * (1 - treino.custoFinal() / treino.custoInicial())));
        texto.append("## Avaliação (").append(SEMENTES_DE_TESTE).append(" meses simulados por cenário)\n\n")
                .append("| Cenário | Agente | Atingimento da meta | Erro médio | Variação diária de preço | Dias saturados |\n")
                .append("|---|---|---|---|---|---|\n");
        for (Avaliacao a : avaliacoes) {
            texto.append(String.format("| %s | %s | %.1f%% ± %.1f | %.3f | %.2f p.p. | %.1f |%n",
                    a.cenario().getDescricao(), a.agente(), 100 * a.atingimentoMedio(), 100 * a.atingimentoDesvio(),
                    a.erroMedio(), 100 * a.variacaoPreco(), a.diasSaturados()));
        }
        return texto.toString();
    }

    private static String linhaGanhos(String rotulo, ControladorPID.Ganhos ganhos, double custo) {
        return String.format("| %s | %.4f | %.4f | %.4f | %.4f |%n", rotulo, ganhos.kp(), ganhos.ki(), ganhos.kd(), custo);
    }

    private static String csvResultados(List<Avaliacao> avaliacoes) {
        StringBuilder csv = new StringBuilder(
                "cenario,agente,atingimento_medio,atingimento_desvio,erro_medio,variacao_preco,dias_saturados\n");
        for (Avaliacao a : avaliacoes) {
            csv.append(String.format("%s,%s,%.4f,%.4f,%.4f,%.4f,%.2f%n", a.cenario().name(), a.agente(),
                    a.atingimentoMedio(), a.atingimentoDesvio(), a.erroMedio(), a.variacaoPreco(), a.diasSaturados()));
        }
        return csv.toString();
    }

    private static String csvConvergencia(CalibradorGanhos.Resultado treino) {
        StringBuilder csv = new StringBuilder("iteracao,custo\n");
        for (int i = 0; i < treino.historicoCusto().size(); i++) {
            csv.append(String.format("%d,%.6f%n", i, treino.historicoCusto().get(i)));
        }
        return csv.toString();
    }

    private String csvTrajetoria(Map<String, ControladorPID.Ganhos> agentes, Cenario cenario, long semente) {
        List<Episodio> episodios = new ArrayList<>();
        agentes.values().forEach(ganhos -> episodios.add(ambiente.simular(ganhos, cenario, semente)));
        List<String> nomes = new ArrayList<>(agentes.keySet());
        StringBuilder csv = new StringBuilder("dia,referencia");
        for (String nome : nomes) {
            String coluna = nome.toLowerCase(Locale.ROOT).replace(' ', '_');
            csv.append(",realizado_").append(coluna).append(",ajuste_").append(coluna);
        }
        csv.append('\n');
        for (int dia = 0; dia < episodios.getFirst().referencia().length; dia++) {
            csv.append(dia + 1).append(String.format(",%.2f", episodios.getFirst().referencia()[dia]));
            for (Episodio episodio : episodios) {
                csv.append(String.format(",%.2f,%.4f", episodio.realizado()[dia], episodio.ajustes()[dia] + 0.0));
            }
            csv.append('\n');
        }
        return csv.toString();
    }
}
