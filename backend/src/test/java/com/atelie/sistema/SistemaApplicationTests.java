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

@SpringBootTest(properties = "spring.datasource.url=${ATELIE_TEST_JDBC_URL}")
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
        var cpf = Long.toString(ThreadLocalRandom.current().nextLong(10000000000L, 100000000000L));
        var cliente = clientes.cadastrarCliente(new ClienteRequestDTO("Cliente teste", "22999999999", "teste@email.com", cpf));
        var peca = pecas.cadastrarPeca(new PecaRequestDTO("Vestido", TipoFluxo.LOCACAO, new BigDecimal("150.00"), "M", "Azul"));
        var inicio = LocalDateTime.of(2030, 10, 10, 10, 0);
        var fim = inicio.plusDays(2);
        var pedido = new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO, List.of(peca.id()),
                new BigDecimal("10.00"), inicio, fim, null, null, null, null, inicio);

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

        ordens.retirar(ordem.id());
        ordens.devolver(ordem.id());
        assertTrue(ordens.disponivelNoPeriodo(peca.id(), inicio, fim));
    }
}
