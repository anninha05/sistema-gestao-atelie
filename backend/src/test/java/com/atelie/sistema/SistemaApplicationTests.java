package com.atelie.sistema;

import com.atelie.sistema.cliente.ClienteService;
import com.atelie.sistema.cliente.dto.ClienteRequestDTO;
import com.atelie.sistema.ordemservico.MetodoPagamento;
import com.atelie.sistema.ordemservico.OrdemServicoService;
import com.atelie.sistema.ordemservico.dto.OrdemServicoRequestDTO;
import com.atelie.sistema.ordemservico.dto.RegistrarPagamentoRequest;
import com.atelie.sistema.ordemservico.dto.ItemProducaoRequest;
import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.peca.PecaService;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.peca.dto.PecaRequestDTO;
import com.atelie.sistema.peca.dto.LiberacaoPecaRequestDTO;
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
        "precificacao.agendamento.ativo=false",
        "FRONTEND_ORIGIN=http://localhost:5173",
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
        mvc.perform(get("/ordens-servico").session(autenticada)).andExpect(status().isForbidden());
        mvc.perform(get("/ordens-servico/producao").session(autenticada)).andExpect(status().isOk());
        var clienteTeste = clientes.cadastrarCliente(new ClienteRequestDTO("Cliente produção", "22999999999",
                "cliente.producao@email.com", gerarCpfParaTeste()));
        var pecaTeste = pecas.cadastrarPeca(new PecaRequestDTO("Modelo produção", TipoFluxo.SOB_MEDIDA,
                new BigDecimal("120.00"), "M", "Rosa"));
        var testeOrdem = ordens.abrirOrdemServico(new OrdemServicoRequestDTO(clienteTeste.id(), TipoFluxo.SOB_MEDIDA,
                List.of(pecaTeste.id()), null, null, null, null, null, null, null,
                LocalDateTime.now().plusDays(30), null));
        var primeiraEtapa = obterToken(autenticada);
        mvc.perform(patch("/ordens-servico/" + testeOrdem.id() + "/confeccao").session(autenticada)
                .header("X-CSRF-TOKEN", primeiraEtapa))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusAtual").value("CONFECCAO"))
                .andExpect(jsonPath("$.valorTotal").doesNotExist());
        mvc.perform(get("/pecas").session(autenticada)).andExpect(status().isForbidden());
        mvc.perform(get("/pecas/producao").session(autenticada)).andExpect(status().isOk());
        var idInexistente = UUID.randomUUID();
        mvc.perform(get("/ordens-servico/" + idInexistente + "/contrato.pdf").session(autenticada))
                .andExpect(status().isForbidden());
        mvc.perform(get("/ordens-servico/" + idInexistente + "/pagamentos/1/recibo.pdf").session(autenticada))
                .andExpect(status().isForbidden());
        mvc.perform(get("/financeiro/despesas").session(autenticada)).andExpect(status().isForbidden());
    }

    @Test
    void consolidadoFinanceiroMensalERestritoAGerencia() throws Exception {
        var admin = autenticar(Perfil.ROLE_ADMIN);
        mvc.perform(get("/financeiro/indicadores").session(admin).param("mes", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2026-11"))
                .andExpect(jsonPath("$.resultadoDeCaixa").exists())
                .andExpect(jsonPath("$.saldoTotalAReceber").exists());
        var producao = autenticar(Perfil.ROLE_PRODUCAO);
        mvc.perform(get("/financeiro/indicadores").session(producao).param("mes", "2026-11"))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteGerenciaConfigurarMetaEPoliticaDePrecificacao() throws Exception {
        var admin = autenticar(Perfil.ROLE_ADMIN);
        var mes = java.time.YearMonth.now().plusMonths(3).toString();
        var token = obterToken(admin);
        mvc.perform(put("/precificacao/metas/" + mes).session(admin).header("X-CSRF-TOKEN", token)
                .contentType("application/json")
                .content("""
                        {"valorMeta":12500.00,"indicador":"RESULTADO_DE_CAIXA"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value(mes))
                .andExpect(jsonPath("$.valorMeta").value(12500.00))
                .andExpect(jsonPath("$.responsavelNome").value("Teste retirada"));
        mvc.perform(get("/precificacao/metas/" + mes).session(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.indicador").value("RESULTADO_DE_CAIXA"));

        token = obterToken(admin);
        mvc.perform(put("/precificacao/politica").session(admin).header("X-CSRF-TOKEN", token)
                .contentType("application/json")
                .content("""
                        {"ganhoProporcional":0.4,"ganhoIntegral":0.02,"ganhoDerivativo":0.1,
                         "ajusteMinimo":-0.05,"ajusteMaximo":0.05,
                         "descontoFidelidade":0.1,"quantidadeOsFidelidade":3}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ajusteMinimo").value(-0.05))
                .andExpect(jsonPath("$.quantidadeOsFidelidade").value(3));
        mvc.perform(get("/precificacao/politica").session(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.descontoFidelidade").value(0.1));
    }

    @Test
    void gerenciaCalibraGanhosPorSimulacaoSemAlterarLimitesComerciais() throws Exception {
        var admin = autenticar(Perfil.ROLE_ADMIN);
        mvc.perform(put("/precificacao/politica").session(admin).header("X-CSRF-TOKEN", obterToken(admin))
                .contentType("application/json")
                .content("""
                        {"ganhoProporcional":0.2,"ganhoIntegral":0.01,"ganhoDerivativo":0.05,
                         "ajusteMinimo":-0.2,"ajusteMaximo":0.2}
                        """))
                .andExpect(status().isOk());

        var resposta = mvc.perform(post("/precificacao/politica/calibrar").session(admin)
                        .header("X-CSRF-TOKEN", obterToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpAnterior").value(0.2))
                .andReturn();
        var calibracao = json.readTree(resposta.getResponse().getContentAsString());
        assertTrue(calibracao.get("custo").asDouble() <= calibracao.get("custoAnterior").asDouble());
        mvc.perform(get("/precificacao/politica").session(admin))
                .andExpect(jsonPath("$.ganhoProporcional").value(calibracao.get("kp").asDouble()))
                .andExpect(jsonPath("$.ajusteMinimo").value(-0.2))
                .andExpect(jsonPath("$.ajusteMaximo").value(0.2));

        var atendente = autenticar(Perfil.ROLE_ATENDENTE);
        mvc.perform(post("/precificacao/politica/calibrar").session(atendente)
                        .header("X-CSRF-TOKEN", obterToken(atendente)))
                .andExpect(status().isForbidden());
    }

    @Test
    void perfilProducaoNaoPodeConsultarNemConfigurarPrecificacao() throws Exception {
        var producao = autenticar(Perfil.ROLE_PRODUCAO);
        mvc.perform(get("/precificacao/politica").session(producao)).andExpect(status().isForbidden());
        mvc.perform(get("/precificacao/atualizacoes").session(producao).param("mes", java.time.YearMonth.now().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(put("/precificacao/politica").session(producao)
                .header("X-CSRF-TOKEN", obterToken(producao)).contentType("application/json")
                .content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/precificacao/atualizacao-diaria").session(producao)
                .header("X-CSRF-TOKEN", obterToken(producao)))
                .andExpect(status().isForbidden());
    }

    @Test
    void atualizacaoPidDiariaEIdempotenteEAuditavel() throws Exception {
        var admin = autenticar(Perfil.ROLE_ADMIN);
        var mesAtual = java.time.YearMonth.now();
        var token = obterToken(admin);
        mvc.perform(put("/precificacao/metas/" + mesAtual).session(admin).header("X-CSRF-TOKEN", token)
                .contentType("application/json")
                .content("""
                        {"valorMeta":10000.00,"indicador":"RESULTADO_DE_CAIXA"}
                        """))
                .andExpect(status().isOk());
        token = obterToken(admin);
        mvc.perform(put("/precificacao/politica").session(admin).header("X-CSRF-TOKEN", token)
                .contentType("application/json")
                .content("""
                        {"ganhoProporcional":0.1,"ganhoIntegral":0.01,"ganhoDerivativo":0.1,
                         "ajusteMinimo":-0.05,"ajusteMaximo":0.05,
                         "descontoFidelidade":null,"quantidadeOsFidelidade":null}
                        """))
                .andExpect(status().isOk());

        token = obterToken(admin);
        // Salvar a meta já aciona o acompanhamento; a atualização manual posterior deve reutilizá-lo.
        mvc.perform(put("/precificacao/metas/" + mesAtual).session(admin).header("X-CSRF-TOKEN", token)
                .contentType("application/json")
                .content("{\"valorMeta\":10000.00,\"indicador\":\"RESULTADO_DE_CAIXA\"}"))
                .andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from precificacao_execucoes_pid where competencia = ? and data_apuracao = ?",
                Integer.class, mesAtual.atDay(1), LocalDate.now()));
        var primeira = mvc.perform(post("/precificacao/atualizacao-diaria").session(admin)
                .header("X-CSRF-TOKEN", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value(mesAtual.toString()))
                .andExpect(jsonPath("$.dataApuracao").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.referenciaAcumulada").exists())
                .andExpect(jsonPath("$.ajusteLimitado").exists()).andReturn();
        long idPrimeira = json.readTree(primeira.getResponse().getContentAsString()).get("id").asLong();
        var segunda = mvc.perform(post("/precificacao/atualizacao-diaria").session(admin)
                .header("X-CSRF-TOKEN", token)).andExpect(status().isOk()).andReturn();
        long idSegunda = json.readTree(segunda.getResponse().getContentAsString()).get("id").asLong();

        assertEquals(idPrimeira, idSegunda);
        mvc.perform(get("/precificacao/atualizacoes").session(admin).param("mes", mesAtual.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(idPrimeira))
                .andExpect(jsonPath("$[0].responsavelNome").value("Teste retirada"));
    }

    @Test
    void simulaERegistraSugestaoSemAlterarValorDaOrdem() throws Exception {
        var cliente = clientes.cadastrarCliente(new ClienteRequestDTO("Cliente PID", "22999999999",
                "pid-" + UUID.randomUUID() + "@email.com", gerarCpfParaTeste()));
        var peca = pecas.cadastrarPeca(new PecaRequestDTO("Peça PID", TipoFluxo.LOCACAO,
                new BigDecimal("150.00"), "M", "Rosa"));
        var ordem = ordens.abrirOrdemServico(new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO,
                List.of(peca.id()), null, LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(12),
                null, null, null, null, LocalDateTime.now().plusDays(11), null));
        var valorOriginal = ordem.valorTotal();
        var admin = autenticar(Perfil.ROLE_ADMIN);
        var token = obterToken(admin);
        mvc.perform(put("/precificacao/politica").session(admin).header("X-CSRF-TOKEN", token)
                .contentType("application/json")
                .content("""
                        {"ganhoProporcional":0.1,"ganhoIntegral":0,"ganhoDerivativo":0,
                         "ajusteMinimo":-0.05,"ajusteMaximo":0.05,
                         "descontoFidelidade":null,"quantidadeOsFidelidade":null}
                        """))
                .andExpect(status().isOk());

        String pedido = "{\"ordemServicoId\":\"" + ordem.id() + "\",\"pisoMinimo\":100.00}";
        token = obterToken(admin);
        var simulacao = mvc.perform(post("/precificacao/simular").session(admin).header("X-CSRF-TOKEN", token)
                .contentType("application/json").content(pedido))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.precoBase").value(150.00))
                .andExpect(jsonPath("$.precoSugerido").exists()).andReturn();
        assertEquals(valorOriginal, ordens.buscarPorId(ordem.id()).valorTotal());

        token = obterToken(admin);
        var criada = mvc.perform(post("/precificacao/sugestoes").session(admin).header("X-CSRF-TOKEN", token)
                .contentType("application/json").content(pedido))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.justificativa").isNotEmpty()).andReturn();
        long id = json.readTree(criada.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/precificacao/sugestoes/" + id).session(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ordemServicoId").value(ordem.id().toString()));
        assertEquals(valorOriginal, ordens.buscarPorId(ordem.id()).valorTotal());
        assertTrue(json.readTree(simulacao.getResponse().getContentAsString()).get("id").isNull());
    }

    @Test
    void aprovaSugestaoComAtualizacaoConsistenteDoPagamentoEAuditoria() throws Exception {
        var cliente = clientes.cadastrarCliente(new ClienteRequestDTO("Cliente aprovação", "22999999999",
                "aprovacao-" + UUID.randomUUID() + "@email.com", gerarCpfParaTeste()));
        var peca = pecas.cadastrarPeca(new PecaRequestDTO("Peça aprovação", TipoFluxo.LOCACAO,
                new BigDecimal("150.00"), "M", "Rosa"));
        var ordem = ordens.abrirOrdemServico(new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO,
                List.of(peca.id()), null, LocalDateTime.now().plusDays(15), LocalDateTime.now().plusDays(17),
                null, null, null, null, LocalDateTime.now().plusDays(16), null));
        ordens.registrarPagamento(ordem.id(), new RegistrarPagamentoRequest(new BigDecimal("40.00"),
                LocalDate.now(), MetodoPagamento.PIX));

        var admin = autenticar(Perfil.ROLE_ADMIN);
        var token = obterToken(admin);
        mvc.perform(put("/precificacao/politica").session(admin).header("X-CSRF-TOKEN", token)
                .contentType("application/json")
                .content("""
                        {"ganhoProporcional":0,"ganhoIntegral":0,"ganhoDerivativo":0,
                         "ajusteMinimo":-0.20,"ajusteMaximo":0.20,
                         "descontoFidelidade":null,"quantidadeOsFidelidade":null}
                        """))
                .andExpect(status().isOk());
        var criar = "{\"ordemServicoId\":\"" + ordem.id() + "\",\"pisoMinimo\":180.00}";
        token = obterToken(admin);
        var criada = mvc.perform(post("/precificacao/sugestoes").session(admin).header("X-CSRF-TOKEN", token)
                .contentType("application/json").content(criar))
                .andExpect(status().isOk()).andReturn();
        long sugestaoId = json.readTree(criada.getResponse().getContentAsString()).get("id").asLong();

        token = obterToken(admin);
        mvc.perform(post("/ordens-servico/" + ordem.id() + "/aprovar-preco").session(admin)
                .header("X-CSRF-TOKEN", token).contentType("application/json")
                .content("{\"sugestaoId\":" + sugestaoId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorAnterior").value(150.00))
                .andExpect(jsonPath("$.valorAprovado").value(180.00))
                .andExpect(jsonPath("$.valorRecebido").value(40.00))
                .andExpect(jsonPath("$.responsavelNome").value("Teste retirada"));
        assertEquals(new BigDecimal("180.00"), ordens.buscarPorId(ordem.id()).valorTotal());
        mvc.perform(get("/ordens-servico/" + ordem.id() + "/pagamentos").session(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorTotal").value(180.00))
                .andExpect(jsonPath("$.valorPago").value(40.00)).andExpect(jsonPath("$.saldoDevedor").value(140.00));
        assertEquals(1, jdbc.queryForObject("select count(*) from precificacao_aprovacoes where sugestao_id = ?",
                Integer.class, sugestaoId));

        token = obterToken(admin);
        mvc.perform(post("/ordens-servico/" + ordem.id() + "/aprovar-preco").session(admin)
                .header("X-CSRF-TOKEN", token).contentType("application/json")
                .content("{\"sugestaoId\":" + sugestaoId + "}"))
                .andExpect(status().isBadRequest());
        assertEquals(1, jdbc.queryForObject("select count(*) from precificacao_aprovacoes where sugestao_id = ?",
                Integer.class, sugestaoId));

        var outraPeca = pecas.cadastrarPeca(new PecaRequestDTO("Peça outra ordem", TipoFluxo.LOCACAO,
                new BigDecimal("125.00"), "M", "Azul"));
        var outra = ordens.abrirOrdemServico(new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO,
                List.of(outraPeca.id()), null, LocalDateTime.now().plusDays(20), LocalDateTime.now().plusDays(22),
                null, null, null, null, LocalDateTime.now().plusDays(21), null));
        token = obterToken(admin);
        mvc.perform(post("/ordens-servico/" + outra.id() + "/aprovar-preco").session(admin)
                .header("X-CSRF-TOKEN", token).contentType("application/json")
                .content("{\"sugestaoId\":" + sugestaoId + "}"))
                .andExpect(status().isBadRequest());
        assertEquals(new BigDecimal("125.00"), ordens.buscarPorId(outra.id()).valorTotal());

        var producao = autenticar(Perfil.ROLE_PRODUCAO);
        mvc.perform(post("/ordens-servico/" + outra.id() + "/aprovar-preco").session(producao)
                .header("X-CSRF-TOKEN", obterToken(producao)).contentType("application/json")
                .content("{\"sugestaoId\":" + sugestaoId + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void fluxoSimplificadoCadastraMinimoSugereSemAlterarEConfirma() throws Exception {
        // Remove somente a configuração dentro da transação de teste, revertida ao final.
        jdbc.update("delete from precificacao_politica");
        var cliente = clientes.cadastrarCliente(new ClienteRequestDTO("Cliente sugestão simples", "22999999999",
                "simples-" + UUID.randomUUID() + "@email.com", gerarCpfParaTeste()));
        var peca = pecas.cadastrarPeca(new PecaRequestDTO("Peça sugestão simples", TipoFluxo.LOCACAO,
                new BigDecimal("150.00"), "M", "Rosa"));
        var ordem = ordens.abrirOrdemServico(new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO,
                List.of(peca.id()), null, LocalDateTime.now().plusDays(25), LocalDateTime.now().plusDays(27),
                null, null, null, null, LocalDateTime.now().plusDays(26), null));
        var admin = autenticar(Perfil.ROLE_ADMIN);
        String token = obterToken(admin);
        mvc.perform(put("/precificacao/metas/" + java.time.YearMonth.now()).session(admin)
                .header("Origin", "http://localhost:5173")
                .header("X-CSRF-TOKEN", token).contentType("application/json")
                .content("{\"valorMeta\":5000,\"indicador\":\"RESULTADO_DE_CAIXA\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/precificacao/politica").session(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ajusteMinimo").value(-0.20))
                .andExpect(jsonPath("$.ajusteMaximo").value(0.20));
        String pedido = "{\"ordemServicoId\":\"" + ordem.id() + "\"}";
        token = obterToken(admin);
        mvc.perform(post("/precificacao/sugestoes").session(admin).header("X-CSRF-TOKEN", token)
                .contentType("application/json").content(pedido)).andExpect(status().isBadRequest());
        String caminhoPiso = "/precificacao/ordens/" + ordem.id() + "/preco-minimo";
        mvc.perform(put(caminhoPiso).session(admin).header("Origin", "http://localhost:5173").header("X-CSRF-TOKEN", token)
                .contentType("application/json").content("{\"valor\":0}")).andExpect(status().isBadRequest());
        mvc.perform(put(caminhoPiso).session(admin).header("Origin", "http://localhost:5173").header("X-CSRF-TOKEN", token)
                .contentType("application/json").content("{\"valor\":120}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valor").value(120));
        mvc.perform(get(caminhoPiso).session(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(120));
        var gerada = mvc.perform(post("/precificacao/sugestoes").session(admin).header("X-CSRF-TOKEN", token)
                .contentType("application/json").content(pedido)).andExpect(status().isOk())
                .andExpect(jsonPath("$.pisoMinimo").value(120)).andReturn();
        var resultado = json.readTree(gerada.getResponse().getContentAsString());
        long sugestaoId = resultado.get("id").asLong();
        var sugerido = new BigDecimal(resultado.get("precoSugerido").asString());
        assertTrue(sugerido.compareTo(new BigDecimal("120")) >= 0);
        assertEquals(new BigDecimal("150.00"), ordens.buscarPorId(ordem.id()).valorTotal());
        mvc.perform(post("/ordens-servico/" + ordem.id() + "/aprovar-preco").session(admin)
                .header("X-CSRF-TOKEN", token).contentType("application/json")
                .content("{\"sugestaoId\":" + sugestaoId + "}"))
                .andExpect(status().isOk());
        assertEquals(0, sugerido.compareTo(ordens.buscarPorId(ordem.id()).valorTotal()));
    }

    @Test
    void precoMinimoSoPodeSerCadastradoPelaGerencia() throws Exception {
        for (var perfil : List.of(Perfil.ROLE_ATENDENTE, Perfil.ROLE_PRODUCAO)) {
            var sessao = autenticar(perfil);
            mvc.perform(put("/precificacao/ordens/" + UUID.randomUUID() + "/preco-minimo").session(sessao)
                    .header("X-CSRF-TOKEN", obterToken(sessao)).contentType("application/json")
                    .content("{\"valor\":100}")).andExpect(status().isForbidden());
        }
    }

    @Test
    void devePersistirReservaFluxoERecebimento() throws Exception {
        var cpf = gerarCpfParaTeste();
        var cliente = clientes.cadastrarCliente(new ClienteRequestDTO("Cliente teste", "22999999999", "teste@email.com", cpf));
        var peca = pecas.cadastrarPeca(new PecaRequestDTO("Vestido", TipoFluxo.LOCACAO, new BigDecimal("150.00"), "M", "Azul"));
        var inicio = LocalDateTime.of(2030, 10, 10, 10, 0);
        var fim = inicio.plusDays(2);
        var pedido = new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO, List.of(peca.id()),
                new BigDecimal("10.00"), inicio, fim, null, null, null, null, inicio, null,
                List.of(new ItemProducaoRequest(peca.id(), "Busto 92 cm", "Ajustar barra 3 cm")));

        var ordem = ordens.abrirOrdemServico(pedido);
        assertEquals(new BigDecimal("140.00"), ordem.valorTotal());
        assertEquals(peca.id(), ordens.buscarPorId(ordem.id()).itens().getFirst().pecaId());
        assertEquals("Busto 92 cm", ordens.buscarPorId(ordem.id()).itens().getFirst().medidas());
        assertEquals("Ajustar barra 3 cm", ordens.buscarPorId(ordem.id()).itens().getFirst().instrucoesCostura());
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
        conferirEstados(ordem, StatusOrdemServico.DEVOLVIDA, StatusDisponibilidade.EM_LAVAGEM);
        assertTrue(ordens.disponivelNoPeriodo(peca.id(), inicio, fim));

        var pedidoPercentual = new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO, List.of(peca.id()),
                null, inicio, fim, null, null, null, null, inicio, new BigDecimal("6.67"));
        var ordemComPercentual = ordens.abrirOrdemServico(pedidoPercentual);
        assertEquals(new BigDecimal("10.01"), ordemComPercentual.desconto());
        assertEquals(new BigDecimal("139.99"), ordemComPercentual.valorTotal());

        var sessao = autenticar(Perfil.ROLE_ATENDENTE);
        mvc.perform(get("/clientes/" + cliente.id() + "/ordens-servico").session(sessao))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '" + ordem.id() + "')]").isArray());
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
    void devePermitirReservasSemSobreposicaoParaMesmaPeca() {
        var cliente = clientes.cadastrarCliente(new ClienteRequestDTO("Datas distintas", "22999999999", "datas@email.com", gerarCpfParaTeste()));
        var peca = pecas.cadastrarPeca(new PecaRequestDTO("Peça agenda", TipoFluxo.LOCACAO, new BigDecimal("120.00"), "M", "Azul"));
        var inicio = LocalDateTime.now().plusDays(30);
        var primeira = new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO, List.of(peca.id()), null,
                inicio, inicio.plusDays(2), null, null, null, null, inicio, null);
        var segunda = new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO, List.of(peca.id()), null,
                inicio.plusDays(2), inicio.plusDays(4), null, null, null, null, inicio, null);
        assertNotNull(ordens.abrirOrdemServico(primeira).id());
        assertNotNull(ordens.abrirOrdemServico(segunda).id());
        assertTrue(ordens.disponivelNoPeriodo(peca.id(), inicio.plusDays(4), inicio.plusDays(5)));
    }

    @Test
    void devePermitirReservaFuturaComPecaEmLavagemMasImpedirInicioImediato() {
        var cliente = clientes.cadastrarCliente(new ClienteRequestDTO("Peça em lavagem", "22999999999", "lavagem@email.com", gerarCpfParaTeste()));
        var peca = pecas.cadastrarPeca(new PecaRequestDTO("Peça agenda", TipoFluxo.LOCACAO, new BigDecimal("120.00"), "M", "Azul"));
        pecas.enviarParaLavagem(peca.id());
        var agora = LocalDateTime.now();
        var imediata = new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO, List.of(peca.id()), null,
                agora.minusMinutes(1), agora.plusDays(1), null, null, null, null, agora, null);
        assertThrows(RegraNegocioException.class, () -> ordens.abrirOrdemServico(imediata));
        assertFalse(ordens.disponivelNoPeriodo(peca.id(), agora, agora.plusDays(1)));
        var futura = new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO, List.of(peca.id()), null,
                agora.plusDays(10), agora.plusDays(12), null, null, null, null, agora, null);
        assertNotNull(ordens.abrirOrdemServico(futura).id());
        assertEquals(StatusDisponibilidade.EM_LAVAGEM, pecas.buscarPorId(peca.id()).status());
    }

    @Test
    void deveImpedirDuasReservasSimultaneasDaMesmaPeca() throws Exception {
        var cliente = clientes.cadastrarCliente(new ClienteRequestDTO("Reserva concorrente", "22999999999", "concorrente@email.com", gerarCpfParaTeste()));
        var peca = pecas.cadastrarPeca(new PecaRequestDTO("Peça concorrente", TipoFluxo.LOCACAO, new BigDecimal("120.00"), "M", "Azul"));
        var inicio = LocalDateTime.now().plusDays(30);
        var pedido = new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO, List.of(peca.id()), null,
                inicio, inicio.plusDays(2), null, null, null, null, inicio, null);
        var prontas = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> reservar = () -> {
                prontas.countDown();
                if (!iniciar.await(10, TimeUnit.SECONDS)) throw new AssertionError("Reservas não iniciaram");
                try { ordens.abrirOrdemServico(pedido); return true; }
                catch (RegraNegocioException esperado) { return false; }
            };
            var primeira = executor.submit(reservar);
            var segunda = executor.submit(reservar);
            assertTrue(prontas.await(10, TimeUnit.SECONDS));
            iniciar.countDown();
            assertNotEquals(primeira.get(20, TimeUnit.SECONDS), segunda.get(20, TimeUnit.SECONDS));
        }
        assertEquals(1, jdbc.queryForObject("select count(*) from ordens_servico_pecas i join ordens_servico os on os.id = i.ordem_servico_id where i.peca_id = ? and os.status_atual not in ('DEVOLVIDA', 'CANCELADA')", Integer.class, peca.id()));
    }

    @Test
    void deveBloquearRotasDeAluguelEDevolucaoDiretosEManterManutencaoAutorizada() throws Exception {
        var peca = pecas.cadastrarPeca(new PecaRequestDTO("Ação direta", TipoFluxo.LOCACAO, new BigDecimal("120.00"), "M", "Azul"));
        var atendimento = autenticar(Perfil.ROLE_ATENDENTE);
        var producao = autenticar(Perfil.ROLE_PRODUCAO);
        var tokenAtendimento = obterToken(atendimento);
        var tokenProducao = obterToken(producao);
        mvc.perform(patch("/pecas/" + peca.id() + "/alugar").session(atendimento).header("X-CSRF-TOKEN", tokenAtendimento))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/pecas/" + peca.id() + "/devolver").session(atendimento).header("X-CSRF-TOKEN", tokenAtendimento)
                .contentType("application/json").content("{\"lavagemConcluida\":true,\"condicoesConferidas\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/pecas/" + peca.id() + "/costura").session(producao).header("X-CSRF-TOKEN", tokenProducao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_COSTURA"));
        mvc.perform(patch("/pecas/" + peca.id() + "/costura").session(atendimento).header("X-CSRF-TOKEN", tokenAtendimento))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveCancelarComPendenciaPreservarRecebimentosELiberarReserva() {
        var cliente = clientes.cadastrarCliente(new ClienteRequestDTO("Cancelar", "22999999999", "cancelar@email.com", gerarCpfParaTeste()));
        var peca = pecas.cadastrarPeca(new PecaRequestDTO("Peça cancelar", TipoFluxo.LOCACAO, new BigDecimal("120.00"), "M", "Azul"));
        var inicio = LocalDateTime.now().plusDays(30);
        var pedido = new OrdemServicoRequestDTO(cliente.id(), TipoFluxo.LOCACAO, List.of(peca.id()), null,
                inicio, inicio.plusDays(2), null, null, null, null, inicio, null);
        var ordem = ordens.abrirOrdemServico(pedido);
        ordens.registrarSinal(ordem.id(), new RegistrarPagamentoRequest(new BigDecimal("30.00"), LocalDate.now(), MetodoPagamento.PIX));
        var cancelada = ordens.cancelar(ordem.id());
        assertEquals(StatusOrdemServico.CANCELADA, cancelada.statusAtual());
        assertTrue(cancelada.pendenciaAcerto());
        assertTrue(ordens.disponivelNoPeriodo(peca.id(), inicio, inicio.plusDays(2)));
        assertEquals(new BigDecimal("30.00"), ordens.buscarPagamento(ordem.id()).valorPago());
        assertEquals(1, ordens.buscarPagamento(ordem.id()).recebimentos().size());
        assertThrows(RegraNegocioException.class, () -> ordens.retirar(ordem.id()));
        assertThrows(RegraNegocioException.class, () -> ordens.registrarPagamento(ordem.id(),
                new RegistrarPagamentoRequest(BigDecimal.ONE, LocalDate.now(), MetodoPagamento.PIX)));
        assertEquals(1, ordens.buscarPagamento(ordem.id()).recebimentos().size());
        assertThrows(RegraNegocioException.class, () -> ordens.cancelar(ordem.id()));
    }

    @Test
    void deveRejeitarLocacaoSemItens() {
        var ordem = criarOrdemPronta(TipoFluxo.LOCACAO, 1);
        quitar(ordem);
        jdbc.update("delete from ordens_servico_pecas where ordem_servico_id = ?", ordem.id());
        assertThrows(RegraNegocioException.class, () -> ordens.retirar(ordem.id()));
        conferirEstados(ordem, StatusOrdemServico.CONCLUIDO, StatusDisponibilidade.DISPONIVEL);
    }

    @Test
    void deveDevolverTodasAsPecasParaLavagemSemLiberarEstoque() {
        var ordem = criarLocacaoRetirada(2);
        assertEquals(StatusOrdemServico.DEVOLVIDA, ordens.devolver(ordem.id()).statusAtual());
        conferirEstados(ordem, StatusOrdemServico.DEVOLVIDA, StatusDisponibilidade.EM_LAVAGEM);
        for (var item : ordem.itens()) {
            assertNotEquals(StatusDisponibilidade.DISPONIVEL, pecas.buscarPorId(item.pecaId()).status());
        }
        assertThrows(RegraNegocioException.class, () -> ordens.devolver(ordem.id()));
        conferirEstados(ordem, StatusOrdemServico.DEVOLVIDA, StatusDisponibilidade.EM_LAVAGEM);
    }

    @ParameterizedTest
    @EnumSource(value = StatusOrdemServico.class, names = {"RETIRADA"}, mode = EnumSource.Mode.EXCLUDE)
    void deveRejeitarDevolucaoForaDoEstadoRetirada(StatusOrdemServico estado) {
        var ordem = criarOrdemPronta(TipoFluxo.LOCACAO, 1);
        jdbc.update("update ordens_servico set status_atual = ? where id = ?", estado.name(), ordem.id());
        assertThrows(RegraNegocioException.class, () -> ordens.devolver(ordem.id()));
        conferirEstados(ordem, estado, StatusDisponibilidade.DISPONIVEL);
    }

    @Test
    void deveReverterDevolucaoSeUmaPecaEstiverInconsistente() {
        var ordem = criarLocacaoRetirada(2);
        var ids = ordem.itens().stream().map(item -> item.pecaId()).sorted().toList();
        jdbc.update("update pecas set estado_atual = 'DISPONIVEL' where id = ?", ids.getLast());
        assertThrows(RegraNegocioException.class, () -> ordens.devolver(ordem.id()));
        // Sem transação no teste: estas consultas leem o resultado persistido após o rollback.
        assertEquals(StatusOrdemServico.RETIRADA, ordens.buscarPorId(ordem.id()).statusAtual());
        assertEquals(StatusDisponibilidade.ALUGADA, pecas.buscarPorId(ids.getFirst()).status());
        assertEquals(StatusDisponibilidade.DISPONIVEL, pecas.buscarPorId(ids.getLast()).status());
    }

    @Test
    void deveRejeitarDevolucaoSemItens() {
        var ordem = criarLocacaoRetirada(1);
        jdbc.update("delete from ordens_servico_pecas where ordem_servico_id = ?", ordem.id());
        assertThrows(RegraNegocioException.class, () -> ordens.devolver(ordem.id()));
        conferirEstados(ordem, StatusOrdemServico.RETIRADA, StatusDisponibilidade.ALUGADA);
    }

    @Test
    void devePreservarReservaFuturaEBloquearRetiradaAteLiberacao() {
        var anterior = criarLocacaoRetirada(1);
        var pecaId = anterior.itens().getFirst().pecaId();
        var inicio = anterior.fimLocacao().plusDays(2);
        var futura = ordens.abrirOrdemServico(new OrdemServicoRequestDTO(anterior.clienteId(), TipoFluxo.LOCACAO,
                List.of(pecaId), null, inicio, inicio.plusDays(2), null, null, null, null, inicio, null));
        ordens.enviarParaProva(futura.id());
        ordens.iniciarPassadoria(futura.id());
        ordens.concluir(futura.id());
        quitar(futura);

        ordens.devolver(anterior.id());
        assertEquals(inicio, ordens.buscarPorId(futura.id()).inicioLocacao());
        assertEquals(futura.fimLocacao(), ordens.buscarPorId(futura.id()).fimLocacao());
        assertFalse(ordens.disponivelNoPeriodo(pecaId, inicio, futura.fimLocacao()));
        assertThrows(RegraNegocioException.class, () -> ordens.retirar(futura.id()));
        conferirEstados(futura, StatusOrdemServico.CONCLUIDO, StatusDisponibilidade.EM_LAVAGEM);
        assertThrows(RegraNegocioException.class, () -> pecas.devolverParaEstoque(pecaId, null));
        assertThrows(RegraNegocioException.class, () -> pecas.devolverParaEstoque(pecaId,
                new LiberacaoPecaRequestDTO(false, true)));
        assertThrows(RegraNegocioException.class, () -> pecas.devolverParaEstoque(pecaId,
                new LiberacaoPecaRequestDTO(true, false)));

        pecas.enviarParaCostura(pecaId);
        assertThrows(RegraNegocioException.class, () -> ordens.retirar(futura.id()));
        conferirEstados(futura, StatusOrdemServico.CONCLUIDO, StatusDisponibilidade.EM_COSTURA);
        pecas.enviarParaLavagem(pecaId);
        pecas.devolverParaEstoque(pecaId, new LiberacaoPecaRequestDTO(true, true));
        conferirEstados(futura, StatusOrdemServico.CONCLUIDO, StatusDisponibilidade.DISPONIVEL);
        assertEquals(StatusOrdemServico.RETIRADA, ordens.retirar(futura.id()).statusAtual());
        conferirEstados(futura, StatusOrdemServico.RETIRADA, StatusDisponibilidade.ALUGADA);
        assertEquals(StatusOrdemServico.DEVOLVIDA, ordens.buscarPorId(anterior.id()).statusAtual());
    }

    @Test
    void deveManterManutencaoIndisponivelAteConferenciaExplicita() {
        var ordem = criarLocacaoRetirada(1);
        ordens.devolver(ordem.id());
        var pecaId = ordem.itens().getFirst().pecaId();
        jdbc.update("update pecas set estado_atual = 'MANUTENCAO' where id = ?", pecaId);
        assertThrows(RegraNegocioException.class, () -> pecas.devolverParaEstoque(pecaId,
                new LiberacaoPecaRequestDTO(true, false)));
        assertEquals(StatusDisponibilidade.MANUTENCAO, pecas.buscarPorId(pecaId).status());
        pecas.devolverParaEstoque(pecaId, new LiberacaoPecaRequestDTO(true, true));
        conferirEstados(ordem, StatusOrdemServico.DEVOLVIDA, StatusDisponibilidade.DISPONIVEL);
    }

    @Test
    void deveConfirmarSomenteUmaDevolucaoSimultanea() throws Exception {
        var ordem = criarLocacaoRetirada(2);
        var prontas = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> devolver = () -> {
                prontas.countDown();
                if (!iniciar.await(10, TimeUnit.SECONDS)) throw new AssertionError("Devoluções não iniciaram");
                try {
                    ordens.devolver(ordem.id());
                    return true;
                } catch (RegraNegocioException esperado) {
                    return false;
                }
            };
            var primeira = executor.submit(devolver);
            var segunda = executor.submit(devolver);
            assertTrue(prontas.await(10, TimeUnit.SECONDS));
            iniciar.countDown();
            assertNotEquals(primeira.get(20, TimeUnit.SECONDS), segunda.get(20, TimeUnit.SECONDS));
        }
        conferirEstados(ordem, StatusOrdemServico.DEVOLVIDA, StatusDisponibilidade.EM_LAVAGEM);
    }

    @Test
    void deveIntegrarDevolucaoELiberacaoPelaApiComPermissoesECsrf() throws Exception {
        var ordem = criarLocacaoRetirada(1);
        var pecaId = ordem.itens().getFirst().pecaId();
        var atendimento = autenticar(Perfil.ROLE_ATENDENTE);
        var producao = autenticar(Perfil.ROLE_PRODUCAO);
        var tokenAtendimento = obterToken(atendimento);
        var tokenProducao = obterToken(producao);
        var devolucao = "/ordens-servico/" + ordem.id() + "/devolver";
        var liberacao = "/pecas/" + pecaId + "/devolver";
        var confirmacao = "{\"lavagemConcluida\":true,\"condicoesConferidas\":true}";

        mvc.perform(patch(devolucao).session(atendimento)).andExpect(status().isForbidden());
        mvc.perform(patch(devolucao).session(producao).header("X-CSRF-TOKEN", tokenProducao))
                .andExpect(status().isForbidden());
        mvc.perform(patch(liberacao).session(atendimento).header("X-CSRF-TOKEN", tokenAtendimento)
                .contentType("application/json").content(confirmacao)).andExpect(status().isBadRequest());
        mvc.perform(patch("/pecas/" + pecaId + "/lavagem").session(producao).header("X-CSRF-TOKEN", tokenProducao))
                .andExpect(status().isBadRequest());
        conferirEstados(ordem, StatusOrdemServico.RETIRADA, StatusDisponibilidade.ALUGADA);

        mvc.perform(patch(devolucao).session(atendimento).header("X-CSRF-TOKEN", tokenAtendimento))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusAtual").value("DEVOLVIDA"));
        mvc.perform(get("/pecas/" + pecaId).session(producao)).andExpect(status().isForbidden());
        mvc.perform(get("/pecas/producao").session(producao))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '" + pecaId + "')].status").value("EM_LAVAGEM"));
        mvc.perform(patch(devolucao).session(atendimento).header("X-CSRF-TOKEN", tokenAtendimento))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(liberacao).session(producao).contentType("application/json").content(confirmacao))
                .andExpect(status().isForbidden());
        for (String corpo : List.of("{}", "{\"lavagemConcluida\":true}",
                "{\"lavagemConcluida\":true,\"condicoesConferidas\":false}")) {
            mvc.perform(patch(liberacao).session(producao).header("X-CSRF-TOKEN", tokenProducao)
                    .contentType("application/json").content(corpo))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensagem").exists());
        }
        conferirEstados(ordem, StatusOrdemServico.DEVOLVIDA, StatusDisponibilidade.EM_LAVAGEM);
        mvc.perform(patch(liberacao).session(producao).header("X-CSRF-TOKEN", tokenProducao)
                .contentType("application/json").content(confirmacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISPONIVEL"))
                .andExpect(jsonPath("$.valorBase").doesNotExist());
        conferirEstados(ordem, StatusOrdemServico.DEVOLVIDA, StatusDisponibilidade.DISPONIVEL);
    }

    private String obterToken(MockHttpSession sessao) throws Exception {
        var resposta = mvc.perform(get("/auth/csrf").session(sessao)).andExpect(status().isOk()).andReturn();
        return json.readTree(resposta.getResponse().getContentAsString()).get("token").asString();
    }

    private OrdemServicoResponseDTO criarLocacaoRetirada(int quantidade) {
        var ordem = criarOrdemPronta(TipoFluxo.LOCACAO, quantidade);
        quitar(ordem);
        return ordens.retirar(ordem.id());
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
