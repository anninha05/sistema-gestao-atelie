package com.atelie.sistema;

import com.atelie.sistema.cliente.ClienteService;
import com.atelie.sistema.cliente.dto.ClienteRequestDTO;
import com.atelie.sistema.ordemservico.MetodoPagamento;
import com.atelie.sistema.ordemservico.OrdemServicoService;
import com.atelie.sistema.ordemservico.dto.OrdemServicoRequestDTO;
import com.atelie.sistema.ordemservico.dto.RegistrarPagamentoRequest;
import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.peca.PecaService;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.peca.dto.PecaRequestDTO;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import com.atelie.sistema.usuario.Perfil;
import com.atelie.sistema.usuario.UsuarioService;
import com.atelie.sistema.usuario.dto.UsuarioRequestDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.atelie.sistema.peca.state.StatusDisponibilidade;
import com.atelie.sistema.ordemservico.dto.OrdemServicoResponseDTO;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]",
        "spring.datasource.url=${ATELIE_TEST_JDBC_URL:jdbc:postgresql://localhost:5432/atelie_test}",
        "ADMIN_EMAIL=",
        "ADMIN_PASSWORD="
})
@AutoConfigureMockMvc
class SistemaApplicationTests {

    @Autowired
    private ClienteService clientes;

    @Autowired
    private PecaService pecas;

    @Autowired
    private OrdemServicoService ordens;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private UsuarioService usuarios;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void deveExigirLoginESepararPermissoes() throws Exception {
        var email = "producao-" + java.util.UUID.randomUUID() + "@atelie.com";
        var senha = "senha-teste-123";
        usuarios.cadastrarUsuario(new UsuarioRequestDTO("Produção", email, senha, Perfil.ROLE_PRODUCAO));

        mvc.perform(get("/ordens-servico")).andExpect(status().isUnauthorized());
        var csrf = mvc.perform(get("/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token = json.readTree(csrf.getResponse().getContentAsString()).get("token").asString();
        var sessao = (MockHttpSession) csrf.getRequest().getSession(false);
        var login = mvc.perform(post("/auth/login").session(sessao).header("X-CSRF-TOKEN", token)
                .param("email", email).param("password", senha))
                .andExpect(status().isNoContent()).andReturn();
        var autenticada = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get("/auth/me").session(autenticada))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(get("/ordens-servico").session(autenticada)).andExpect(status().isOk());
        mvc.perform(get("/financeiro/despesas").session(autenticada)).andExpect(status().isForbidden());
    }

    @Test
    void devePersistirReservaFluxoERecebimento() {
        var cpf = gerarCpfParaTeste();
        var cliente = clientes.cadastrarCliente(new ClienteRequestDTO("Cliente teste", "22999999999", "teste@email.com", cpf));
        var peca = pecas.cadastrarPeca(new PecaRequestDTO("Vestido", TipoFluxo.LOCACAO, new BigDecimal("150.00"), "M", "Azul"));
        var inicio = LocalDateTime.of(2030, 10, 10, 10, 0);
        var fim = inicio.plusDays(2);
        var pedido = new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO, List.of(peca.id()),
                new BigDecimal("10.00"), inicio, fim, null, null, null, null, inicio, null);

        var ordem = ordens.abrirOrdemServico(pedido);
        assertEquals(new BigDecimal("140.00"), ordem.valorTotal());
        assertEquals(peca.id(), ordens.buscarPorId(ordem.id()).itens().getFirst().pecaId());
        assertFalse(ordens.disponivelNoPeriodo(peca.id(), inicio.plusDays(1), fim.plusDays(1)));
        assertTrue(ordens.disponivelNoPeriodo(peca.id(), fim, fim.plusDays(1)));
        assertThrows(RegraNegocioException.class, () -> ordens.abrirOrdemServico(pedido));

        ordens.iniciarConfeccao(ordem.id());
        assertEquals(StatusOrdemServico.AJUSTES, ordens.buscarPorId(ordem.id()).statusAtual());
        ordens.iniciarPassadoria(ordem.id());
        ordens.concluir(ordem.id());
        assertEquals(StatusOrdemServico.CONCLUIDO, ordens.buscarPorId(ordem.id()).statusAtual());

        ordens.registrarSinal(ordem.id(),
                new RegistrarPagamentoRequest(new BigDecimal("40.00"), LocalDate.of(2030, 10, 1), MetodoPagamento.PIX));
        var pagamento = ordens.buscarPagamento(ordem.id());
        assertEquals(new BigDecimal("100.00"), pagamento.saldoDevedor());
        assertEquals(MetodoPagamento.PIX, pagamento.recebimentos().getFirst().metodo());

        assertThrows(RegraNegocioException.class, () -> ordens.retirar(ordem.id()));
        assertEquals(StatusOrdemServico.CONCLUIDO, ordens.buscarPorId(ordem.id()).statusAtual());
        ordens.registrarPagamento(ordem.id(),
                new RegistrarPagamentoRequest(new BigDecimal("100.00"), LocalDate.of(2030, 10, 10), MetodoPagamento.PIX));
        ordens.retirar(ordem.id());
        ordens.devolver(ordem.id());
        assertTrue(ordens.disponivelNoPeriodo(peca.id(), inicio, fim));

        var pedidoPercentual = new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO, List.of(peca.id()),
                null, inicio, fim, null, null, null, null, inicio, new BigDecimal("6.67"));
        var ordemComPercentual = ordens.abrirOrdemServico(pedidoPercentual);
        assertEquals(new BigDecimal("10.01"), ordemComPercentual.desconto());
        assertEquals(new BigDecimal("139.99"), ordemComPercentual.valorTotal());
    }

    @Test
    void deveRetirarTodasAsPecasQuitadasERejeitarRepeticao() {
        var ordem = criarOrdemPronta(TipoFluxo.LOCACAO, 2);
        for (var item : ordem.itens()) {
            assertEquals(StatusDisponibilidade.DISPONIVEL, pecas.buscarPorId(item.pecaId()).status());
        }
        assertThrows(RegraNegocioException.class, () -> ordens.retirar(ordem.id()));
        conferirEstados(ordem, StatusOrdemServico.CONCLUIDO, StatusDisponibilidade.DISPONIVEL);
        quitar(ordem);
        assertEquals(StatusOrdemServico.RETIRADA, ordens.retirar(ordem.id()).statusAtual());
        conferirEstados(ordem, StatusOrdemServico.RETIRADA, StatusDisponibilidade.ALUGADA);
        assertThrows(RegraNegocioException.class, () -> ordens.retirar(ordem.id()));
        conferirEstados(ordem, StatusOrdemServico.RETIRADA, StatusDisponibilidade.ALUGADA);
    }

    @ParameterizedTest
    @EnumSource(value = StatusDisponibilidade.class, names = {"EM_COSTURA", "EM_LAVAGEM", "MANUTENCAO", "ALUGADA"})
    void deveReverterOrdemEPrimeiraPecaSeSegundaNaoEstiverPronta(StatusDisponibilidade estado) {
        var ordem = criarOrdemPronta(TipoFluxo.LOCACAO, 2);
        quitar(ordem);
        var ids = ordem.itens().stream().map(item -> item.pecaId()).sorted().toList();
        jdbc.update("update pecas set estado_atual = ? where id = ?", estado.name(), ids.getLast());

        assertThrows(RegraNegocioException.class, () -> ordens.retirar(ordem.id()));

        // Leituras em novas transações comprovam o rollback no PostgreSQL, não só em memória.
        assertEquals(StatusOrdemServico.CONCLUIDO, ordens.buscarPorId(ordem.id()).statusAtual());
        assertEquals(StatusDisponibilidade.DISPONIVEL, pecas.buscarPorId(ids.getFirst()).status());
        assertEquals(estado, pecas.buscarPorId(ids.getLast()).status());
        assertEquals(0, ordens.buscarPagamento(ordem.id()).saldoDevedor().signum());
    }

    @Test
    void deveEntregarSobMedidaQuitadaSemAlugarModelo() {
        var ordem = criarOrdemPronta(TipoFluxo.SOB_MEDIDA, 1);
        assertThrows(RegraNegocioException.class, () -> ordens.retirar(ordem.id()));
        quitar(ordem);
        assertEquals(StatusOrdemServico.ENTREGUE, ordens.retirar(ordem.id()).statusAtual());
        conferirEstados(ordem, StatusOrdemServico.ENTREGUE, StatusDisponibilidade.DISPONIVEL);
        assertThrows(RegraNegocioException.class, () -> ordens.retirar(ordem.id()));
        assertThrows(RegraNegocioException.class, () -> ordens.devolver(ordem.id()));
    }

    @Test
    void deveRejeitarRetiradaDeOrdemCanceladaMesmoQuitada() {
        var ordem = criarOrdemPronta(TipoFluxo.LOCACAO, 1);
        quitar(ordem);
        ordens.cancelar(ordem.id());
        assertThrows(RegraNegocioException.class, () -> ordens.retirar(ordem.id()));
        conferirEstados(ordem, StatusOrdemServico.CANCELADA, StatusDisponibilidade.DISPONIVEL);
    }

    @Test
    void deveConfirmarSomenteUmaRetiradaSimultanea() throws Exception {
        var ordem = criarOrdemPronta(TipoFluxo.LOCACAO, 2);
        quitar(ordem);
        var prontas = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> retirar = () -> {
                prontas.countDown();
                if (!iniciar.await(10, TimeUnit.SECONDS)) throw new AssertionError("Retiradas não iniciaram");
                try {
                    ordens.retirar(ordem.id());
                    return true;
                } catch (RegraNegocioException esperado) {
                    return false;
                }
            };
            var primeira = executor.submit(retirar);
            var segunda = executor.submit(retirar);
            assertTrue(prontas.await(10, TimeUnit.SECONDS));
            iniciar.countDown();
            assertNotEquals(primeira.get(20, TimeUnit.SECONDS), segunda.get(20, TimeUnit.SECONDS));
        }
        conferirEstados(ordem, StatusOrdemServico.RETIRADA, StatusDisponibilidade.ALUGADA);
    }

    @Test
    void deveExporErroDeQuitacaoNaApiEAutorizarSomenteAtendimento() throws Exception {
        var ordem = criarOrdemPronta(TipoFluxo.LOCACAO, 1);
        var sessao = autenticar(Perfil.ROLE_ATENDENTE);
        var csrf = mvc.perform(get("/auth/csrf").session(sessao)).andReturn();
        var token = json.readTree(csrf.getResponse().getContentAsString()).get("token").asString();
        var caminho = "/ordens-servico/" + ordem.id() + "/retirar";
        mvc.perform(patch(caminho).session(sessao)).andExpect(status().isForbidden());
        mvc.perform(patch(caminho).session(sessao).header("X-CSRF-TOKEN", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Quite o saldo devedor da ordem antes de registrar a retirada ou entrega."));
        quitar(ordem);
        var producao = autenticar(Perfil.ROLE_PRODUCAO);
        var csrfProducao = mvc.perform(get("/auth/csrf").session(producao)).andReturn();
        var tokenProducao = json.readTree(csrfProducao.getResponse().getContentAsString()).get("token").asString();
        mvc.perform(patch(caminho).session(producao).header("X-CSRF-TOKEN", tokenProducao))
                .andExpect(status().isForbidden());
        mvc.perform(patch(caminho).session(sessao).header("X-CSRF-TOKEN", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusAtual").value("RETIRADA"));
        conferirEstados(ordem, StatusOrdemServico.RETIRADA, StatusDisponibilidade.ALUGADA);
    }

    private MockHttpSession autenticar(Perfil perfil) throws Exception {
        var email = "retirada-" + UUID.randomUUID() + "@atelie.com";
        usuarios.cadastrarUsuario(new UsuarioRequestDTO("Teste retirada", email, "senha-teste-123", perfil));
        var csrf = mvc.perform(get("/auth/csrf")).andReturn();
        var token = json.readTree(csrf.getResponse().getContentAsString()).get("token").asString();
        var login = mvc.perform(post("/auth/login").session((MockHttpSession) csrf.getRequest().getSession(false))
                .header("X-CSRF-TOKEN", token).param("email", email).param("password", "senha-teste-123"))
                .andExpect(status().isNoContent()).andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }

    @Test
    void deveRejeitarReservaLegadaConflitanteSemAlterarEstados() {
        var ordem = criarOrdemPronta(TipoFluxo.LOCACAO, 1);
        quitar(ordem);
        var outra = ordens.abrirOrdemServico(new OrdemServicoRequestDTO(ordem.clienteId(), TipoFluxo.LOCACAO,
                ordem.itens().stream().map(item -> item.pecaId()).toList(), null,
                ordem.fimLocacao(), ordem.fimLocacao().plusDays(2), null, null, null, null, null, null));
        // Simula inconsistência legada sem enfraquecer as regras de criação da aplicação.
        jdbc.update("update ordens_servico set inicio_locacao = ? where id = ?", ordem.inicioLocacao(), outra.id());
        assertThrows(RegraNegocioException.class, () -> ordens.retirar(ordem.id()));
        conferirEstados(ordem, StatusOrdemServico.CONCLUIDO, StatusDisponibilidade.DISPONIVEL);
        assertEquals(StatusOrdemServico.RESERVADA, ordens.buscarPorId(outra.id()).statusAtual());
    }

    @Test
    void deveRejeitarLocacaoSemItens() {
        var ordem = criarOrdemPronta(TipoFluxo.LOCACAO, 1);
        quitar(ordem);
        jdbc.update("delete from ordens_servico_pecas where ordem_servico_id = ?", ordem.id());
        assertThrows(RegraNegocioException.class, () -> ordens.retirar(ordem.id()));
        conferirEstados(ordem, StatusOrdemServico.CONCLUIDO, StatusDisponibilidade.DISPONIVEL);
    }

    private OrdemServicoResponseDTO criarOrdemPronta(TipoFluxo tipo, int quantidade) {
        var cliente = clientes.cadastrarCliente(new ClienteRequestDTO("Teste retirada", "22999999999",
                "retirada@email.com", gerarCpfParaTeste()));
        var ids = new java.util.ArrayList<UUID>();
        for (int i = 0; i < quantidade; i++) {
            ids.add(pecas.cadastrarPeca(new PecaRequestDTO("Peça retirada", tipo, new BigDecimal("150.00"), "M", "Rosa")).id());
        }
        var inicio = LocalDateTime.of(2030, 11, 10, 10, 0);
        var ordem = ordens.abrirOrdemServico(new OrdemServicoRequestDTO(cliente.id(), tipo, ids,
                null, inicio, inicio.plusDays(2), null, null, null, null, inicio, null));
        ordens.enviarParaProva(ordem.id());
        ordens.iniciarPassadoria(ordem.id());
        return ordens.concluir(ordem.id());
    }

    private void quitar(OrdemServicoResponseDTO ordem) {
        ordens.registrarPagamento(ordem.id(), new RegistrarPagamentoRequest(ordem.valorTotal(), LocalDate.now(), MetodoPagamento.PIX));
    }

    private void conferirEstados(OrdemServicoResponseDTO ordem, StatusOrdemServico estadoOrdem, StatusDisponibilidade estadoPeca) {
        assertEquals(estadoOrdem, ordens.buscarPorId(ordem.id()).statusAtual());
        for (var item : ordem.itens()) {
            assertEquals(estadoPeca, pecas.buscarPorId(item.pecaId()).status());
        }
    }

    private static String gerarCpfParaTeste() {
        var cpf = new StringBuilder(Long.toString(ThreadLocalRandom.current().nextLong(100000000L, 200000000L)));
        while (cpf.length() < 11) {
            int soma = 0;
            for (int i = 0; i < cpf.length(); i++) {
                soma += (cpf.charAt(i) - '0') * (cpf.length() + 1 - i);
            }
            int resto = soma % 11;
            cpf.append(resto < 2 ? 0 : 11 - resto);
        }
        return cpf.toString();
    }
}
