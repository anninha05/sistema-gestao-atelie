package com.atelie.sistema.precificacao.agente;

import static org.junit.jupiter.api.Assertions.*;

import com.atelie.sistema.precificacao.ControladorPID;
import com.atelie.sistema.precificacao.agente.AmbienteAtelie.Cenario;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AgentePrecificacaoTest {

    private final AmbienteAtelie ambiente = new AmbienteAtelie(AmbienteAtelie.Parametros.PADRAO);
    private final ControladorPID.Ganhos manuais = ExperimentoPrecificacao.GANHOS_MANUAIS;

    @Test
    void agenteGuardaMemoriaEntrePercepcoes() {
        var agente = new AgentePrecificacao(new ControladorPID(), new ControladorPID.Ganhos(0, 1, 0, -1, 1));
        var meta = new BigDecimal("3000.00");
        agente.agir(new AgentePrecificacao.Percepcao(meta, BigDecimal.ZERO, 1, 30, 1));
        var segundo = agente.agir(new AgentePrecificacao.Percepcao(meta, BigDecimal.ZERO, 2, 30, 1));

        assertEquals(0.1, agente.getIntegral(), 1e-12);
        assertEquals(2.0 / 30, agente.getErroAnterior(), 1e-12);
        assertEquals(0.1, segundo.ajusteLimitado(), 1e-12);
    }

    @Test
    void mesmaSementeReproduzOMesmoMes() {
        var primeiro = ambiente.simular(manuais, Cenario.QUEDA_NO_MES, 42);
        var segundo = ambiente.simular(manuais, Cenario.QUEDA_NO_MES, 42);

        assertArrayEquals(primeiro.realizado(), segundo.realizado());
        assertArrayEquals(primeiro.ajustes(), segundo.ajustes());
    }

    @Test
    void semGanhosOAgenteNuncaAlteraOPreco() {
        var episodio = ambiente.simular(manuais.comGanhos(0, 0, 0), Cenario.BAIXA_TEMPORADA, 7);

        for (double ajuste : episodio.ajustes()) {
            assertEquals(0.0, ajuste);
        }
        assertEquals(0.0, episodio.variacaoMediaPreco());
    }

    @Test
    void agenteSobeOPrecoNaBaixaTemporadaEReduzNaAlta() {
        assertTrue(mediaDosAjustes(Cenario.BAIXA_TEMPORADA) > 0);
        assertTrue(mediaDosAjustes(Cenario.ALTA_TEMPORADA) < 0);
    }

    @Test
    void ajusteRespeitaOsLimitesComerciais() {
        var agressivos = manuais.comGanhos(50, 5, 5);
        for (Cenario cenario : Cenario.values()) {
            for (double ajuste : ambiente.simular(agressivos, cenario, 3).ajustes()) {
                assertTrue(ajuste >= manuais.ajusteMinimo() && ajuste <= manuais.ajusteMaximo());
            }
        }
    }

    @Test
    void calibracaoNaoPioraOCustoEMantemGanhosNaoNegativos() {
        var experimento = new ExperimentoPrecificacao(ambiente, new CalibradorGanhos(0.005, 15));
        var resultado = experimento.treinar(manuais);

        assertTrue(resultado.custoFinal() <= resultado.custoInicial());
        assertTrue(resultado.calibrado().kp() >= 0 && resultado.calibrado().ki() >= 0
                && resultado.calibrado().kd() >= 0);
        for (int i = 1; i < resultado.historicoCusto().size(); i++) {
            assertTrue(resultado.historicoCusto().get(i) <= resultado.historicoCusto().get(i - 1));
        }
    }

    @Test
    void calibradorEncontraOMinimoDeUmaFuncaoConhecida() {
        var resultado = new CalibradorGanhos(1e-6, 500).calibrar(manuais, ganhos ->
                Math.pow(ganhos.kp() - 0.7, 2) + Math.pow(ganhos.ki() - 0.05, 2) + Math.pow(ganhos.kd() - 0.3, 2));

        assertEquals(0.7, resultado.calibrado().kp(), 1e-3);
        assertEquals(0.05, resultado.calibrado().ki(), 1e-3);
        assertEquals(0.3, resultado.calibrado().kd(), 1e-3);
    }

    private double mediaDosAjustes(Cenario cenario) {
        double soma = 0;
        int total = 0;
        for (long semente = 1; semente <= 20; semente++) {
            for (double ajuste : ambiente.simular(manuais, cenario, semente).ajustes()) {
                soma += ajuste;
                total++;
            }
        }
        return soma / total;
    }
}
