package com.atelie.sistema.precificacao.agente;

import com.atelie.sistema.precificacao.ControladorPID;
import java.math.BigDecimal;
import java.util.SplittableRandom;

public class AmbienteAtelie {

    public static final double PESO_VARIACAO_PRECO = 0.5;

    public enum Cenario {
        NORMAL("Mês normal", 0.6) {
            double fatorDemanda(int dia, int diasNoMes) {
                return 1.0;
            }
        },
        BAIXA_TEMPORADA("Baixa temporada", 0.6) {
            double fatorDemanda(int dia, int diasNoMes) {
                return 0.8;
            }
        },
        ALTA_TEMPORADA("Alta temporada", 0.6) {
            double fatorDemanda(int dia, int diasNoMes) {
                return 1.2;
            }
        },
        QUEDA_NO_MES("Queda na segunda quinzena", 0.6) {
            double fatorDemanda(int dia, int diasNoMes) {
                return dia <= diasNoMes / 2 ? 1.0 : 0.6;
            }
        },
        CLIENTES_SENSIVEIS_A_PRECO("Clientes sensíveis a preço", 1.5) {
            double fatorDemanda(int dia, int diasNoMes) {
                return 0.8;
            }
        };

        private final String descricao;
        private final double elasticidade;

        Cenario(String descricao, double elasticidade) {
            this.descricao = descricao;
            this.elasticidade = elasticidade;
        }

        abstract double fatorDemanda(int dia, int diasNoMes);

        public String getDescricao() {
            return descricao;
        }

        public double getElasticidade() {
            return elasticidade;
        }
    }

    public record Parametros(int diasNoMes, double pedidosPorDia, double ticketMedio, double despesaMensal) {

        public static final Parametros PADRAO = new Parametros(30, 1.2, 450.0, 6000.0);

        public double meta() {
            return pedidosPorDia * diasNoMes * ticketMedio - despesaMensal;
        }
    }

    public record Episodio(Cenario cenario, double atingimento, double erroMedioAbsoluto, double variacaoMediaPreco,
            int diasSaturados, double[] referencia, double[] realizado, double[] ajustes) {

        public double custo() {
            return erroMedioAbsoluto + PESO_VARIACAO_PRECO * variacaoMediaPreco;
        }
    }

    private final Parametros parametros;
    private final ControladorPID controlador = new ControladorPID();

    public AmbienteAtelie(Parametros parametros) {
        this.parametros = parametros;
    }

    public Episodio simular(ControladorPID.Ganhos ganhos, Cenario cenario, long semente) {
        SplittableRandom raiz = new SplittableRandom(semente);
        AgentePrecificacao agente = new AgentePrecificacao(controlador, ganhos);
        int dias = parametros.diasNoMes();
        double meta = parametros.meta();
        double despesaDiaria = parametros.despesaMensal() / dias;
        double[] referencia = new double[dias];
        double[] realizado = new double[dias];
        double[] ajustes = new double[dias];
        double ajuste = 0;
        double acumulado = 0;
        double somaErros = 0;
        double somaVariacoes = 0;
        int saturados = 0;

        for (int dia = 1; dia <= dias; dia++) {
            double demandaEsperada = parametros.pedidosPorDia() * cenario.fatorDemanda(dia, dias)
                    * Math.pow(1 + ajuste, -cenario.getElasticidade());
            SplittableRandom acaso = raiz.split();
            int pedidos = sortearPoisson(demandaEsperada, acaso.nextDouble());
            for (int i = 0; i < pedidos; i++) {
                acumulado += parametros.ticketMedio() * (1 + ajuste) * (0.7 + 0.6 * acaso.nextDouble());
            }
            acumulado -= despesaDiaria;

            referencia[dia - 1] = meta * dia / dias;
            realizado[dia - 1] = acumulado;
            ajustes[dia - 1] = ajuste;

            ControladorPID.Resultado resultado = agente.agir(new AgentePrecificacao.Percepcao(BigDecimal.valueOf(meta),
                    BigDecimal.valueOf(acumulado), dia, dias, 1));
            somaErros += Math.abs(resultado.erroNormalizado());
            somaVariacoes += Math.abs(resultado.ajusteLimitado() - ajuste);
            if (resultado.ajusteLimitado() <= ganhos.ajusteMinimo() || resultado.ajusteLimitado() >= ganhos.ajusteMaximo()) {
                saturados++;
            }
            ajuste = resultado.ajusteLimitado();
        }
        return new Episodio(cenario, acumulado / meta, somaErros / dias, somaVariacoes / dias, saturados,
                referencia, realizado, ajustes);
    }

    private int sortearPoisson(double media, double sorteio) {
        double probabilidade = Math.exp(-media);
        double acumulada = probabilidade;
        int eventos = 0;
        while (sorteio > acumulada) {
            eventos++;
            probabilidade *= media / eventos;
            acumulada += probabilidade;
        }
        return eventos;
    }
}
