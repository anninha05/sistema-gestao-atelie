package com.atelie.sistema.ordemservico.state;

import static com.atelie.sistema.ordemservico.state.AcaoOrdemServico.*;
import static org.junit.jupiter.api.Assertions.*;

import com.atelie.sistema.cliente.Cliente;
import com.atelie.sistema.ordemservico.MetodoPagamento;
import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import com.atelie.sistema.usuario.Perfil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class AcoesDisponiveisTest {

    private static final Map<AcaoOrdemServico, Consumer<OrdemServico>> OPERACOES = new EnumMap<>(Map.of(
            INICIAR_CONFECCAO, OrdemServico::iniciarConfeccao,
            ENVIAR_PARA_PROVA, OrdemServico::enviarParaProva,
            INICIAR_PASSADORIA, OrdemServico::iniciarPassadoria,
            CONCLUIR, OrdemServico::concluir,
            RETIRAR, OrdemServico::retirar,
            DEVOLVER, OrdemServico::devolver,
            CANCELAR, OrdemServico::cancelar,
            REAGENDAR, os -> os.reagendar(LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(12),
                    null, null, null),
            REGISTRAR_RECEBIMENTO, os -> os.registrarRecebimento(new BigDecimal("1.00"), LocalDate.now(),
                    MetodoPagamento.PIX, false)));

    @Test
    void acoesHabilitadasCorrespondemExatamenteAsTransicoesAceitas() {
        assertEquals(AcaoOrdemServico.values().length, OPERACOES.size(), "Toda ação precisa de uma operação no teste.");
        List<String> divergencias = new ArrayList<>();
        for (StatusOrdemServico status : StatusOrdemServico.values()) {
            for (TipoFluxo tipo : TipoFluxo.values()) {
                for (boolean quitada : new boolean[] {false, true}) {
                    for (AcaoOrdemServico acao : AcaoOrdemServico.values()) {
                        OrdemServico os = ordem(tipo, status, quitada);
                        boolean oferecida = os.acoesDisponiveis().stream()
                                .anyMatch(disponivel -> disponivel.acao() == acao && disponivel.habilitada());
                        boolean aceita = aceita(os, acao);
                        if (oferecida != aceita) {
                            divergencias.add("%s/%s/quitada=%s/%s: oferecida=%s, aceita=%s"
                                    .formatted(status, tipo, quitada, acao, oferecida, aceita));
                        }
                    }
                }
            }
        }
        assertTrue(divergencias.isEmpty(), String.join("\n", divergencias));
    }

    @Test
    void retiradaComSaldoApareceBloqueadaComOMotivo() {
        OrdemServico os = ordem(TipoFluxo.LOCACAO, StatusOrdemServico.CONCLUIDO, false);
        AcaoDisponivel retirada = procurar(os, RETIRAR).orElseThrow();
        assertFalse(retirada.habilitada());
        assertEquals("Registrar retirada", retirada.rotulo());
        assertEquals("Quite o saldo devedor da ordem antes de registrar a retirada ou entrega.", retirada.motivoBloqueio());
        assertTrue(procurar(os, REGISTRAR_RECEBIMENTO).isPresent());

        os.registrarRecebimento(os.getPagamento().getSaldoDevedor(), LocalDate.now(), MetodoPagamento.PIX, false);
        assertTrue(procurar(os, RETIRAR).orElseThrow().habilitada());
        assertTrue(procurar(os, REGISTRAR_RECEBIMENTO).isEmpty());
    }

    @Test
    void rotulosAcompanhamOTipoDaOrdem() {
        assertEquals("Iniciar ajustes", procurar(ordem(TipoFluxo.LOCACAO, StatusOrdemServico.RESERVADA, false),
                INICIAR_CONFECCAO).orElseThrow().rotulo());
        assertEquals("Iniciar confecção", procurar(ordem(TipoFluxo.SOB_MEDIDA, StatusOrdemServico.RESERVADA, false),
                INICIAR_CONFECCAO).orElseThrow().rotulo());
        assertEquals("Registrar entrega", procurar(ordem(TipoFluxo.SOB_MEDIDA, StatusOrdemServico.CONCLUIDO, true),
                RETIRAR).orElseThrow().rotulo());
    }

    @Test
    void estadosEncerradosNaoOferecemAcoes() {
        for (StatusOrdemServico status : List.of(StatusOrdemServico.ENTREGUE, StatusOrdemServico.DEVOLVIDA,
                StatusOrdemServico.CANCELADA)) {
            assertTrue(ordem(TipoFluxo.LOCACAO, status, true).acoesDisponiveis().isEmpty(), status.name());
        }
        assertTrue(ordem(TipoFluxo.LOCACAO, StatusOrdemServico.CANCELADA, false).acoesDisponiveis().isEmpty());
    }

    @Test
    void cadaSetorExecutaApenasAsProprias() {
        for (AcaoOrdemServico acao : AcaoOrdemServico.values()) {
            assertTrue(acao.permitidaPara(Perfil.ROLE_ADMIN), acao.name());
            boolean producao = acao.getSetor() == Setor.PRODUCAO;
            assertEquals(producao, acao.permitidaPara(Perfil.ROLE_PRODUCAO), acao.name());
            assertEquals(!producao, acao.permitidaPara(Perfil.ROLE_ATENDENTE), acao.name());
        }
        assertEquals(Setor.ATENDIMENTO, REGISTRAR_RECEBIMENTO.getSetor());
    }

    private static boolean aceita(OrdemServico os, AcaoOrdemServico acao) {
        try {
            OPERACOES.get(acao).accept(os);
            return true;
        } catch (RegraNegocioException recusada) {
            return false;
        }
    }

    private static Optional<AcaoDisponivel> procurar(OrdemServico os, AcaoOrdemServico acao) {
        return os.acoesDisponiveis().stream().filter(disponivel -> disponivel.acao() == acao).findFirst();
    }

    private static OrdemServico ordem(TipoFluxo tipo, StatusOrdemServico status, boolean quitada) {
        var cliente = new Cliente("Ana", "22999999999", "ana@email.com", "52998224725");
        var os = new OrdemServico(cliente, tipo, new BigDecimal("300.00"), null);
        if (quitada) {
            os.registrarRecebimento(new BigDecimal("300.00"), LocalDate.now(), MetodoPagamento.PIX, false);
        }
        carregarComoGravado(os, status);
        return os;
    }

    private static void carregarComoGravado(OrdemServico ordem, StatusOrdemServico status) {
        try {
            var coluna = OrdemServico.class.getDeclaredField("statusAtual");
            coluna.setAccessible(true);
            coluna.set(ordem, status);
            var aposCarregar = OrdemServico.class.getDeclaredMethod("onPostLoad");
            aposCarregar.setAccessible(true);
            aposCarregar.invoke(ordem);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
