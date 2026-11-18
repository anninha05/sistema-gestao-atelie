package com.atelie.sistema.precificacao;

import com.atelie.sistema.precificacao.dto.MetaMensalRequest;
import com.atelie.sistema.precificacao.dto.MetaMensalResponse;
import com.atelie.sistema.precificacao.dto.PoliticaPrecificacaoRequest;
import com.atelie.sistema.precificacao.dto.PoliticaPrecificacaoResponse;
import com.atelie.sistema.precificacao.dto.ExecucaoPIDResponse;
import com.atelie.sistema.precificacao.dto.SugestaoPrecoRequest;
import com.atelie.sistema.precificacao.dto.SugestaoPrecoResponse;
import java.time.YearMonth;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/precificacao")
class PrecificacaoController {
    private final ConfiguracaoPrecificacaoService configuracao;
    private final AtualizacaoDiariaPIDService atualizacoes;
    private final AgentePrecificacaoService agente;

    PrecificacaoController(ConfiguracaoPrecificacaoService configuracao, AtualizacaoDiariaPIDService atualizacoes,
            AgentePrecificacaoService agente) {
        this.configuracao = configuracao;
        this.atualizacoes = atualizacoes;
        this.agente = agente;
    }

    @GetMapping("/metas/{mes}")
    MetaMensalResponse buscarMeta(@PathVariable YearMonth mes) {
        return configuracao.buscarMeta(mes);
    }

    @PutMapping("/metas/{mes}")
    MetaMensalResponse definirMeta(@PathVariable YearMonth mes, @RequestBody MetaMensalRequest request,
            Authentication autenticacao) {
        return configuracao.definirMeta(mes, request, autenticacao.getName());
    }

    @GetMapping("/politica")
    PoliticaPrecificacaoResponse buscarPolitica() {
        return configuracao.buscarPolitica();
    }

    @PutMapping("/politica")
    PoliticaPrecificacaoResponse configurarPolitica(@RequestBody PoliticaPrecificacaoRequest request,
            Authentication autenticacao) {
        return configuracao.configurarPolitica(request, autenticacao.getName());
    }

    @PostMapping("/atualizacao-diaria")
    ExecucaoPIDResponse atualizarHoje(Authentication autenticacao) {
        return atualizacoes.atualizarHoje(autenticacao.getName());
    }

    @GetMapping("/atualizacoes")
    List<ExecucaoPIDResponse> listarAtualizacoes(@org.springframework.web.bind.annotation.RequestParam YearMonth mes) {
        return atualizacoes.listar(mes);
    }

    @org.springframework.web.bind.annotation.PostMapping("/simular")
    SugestaoPrecoResponse simular(@RequestBody SugestaoPrecoRequest request, Authentication autenticacao) {
        return agente.simular(request, autenticacao.getName());
    }

    @org.springframework.web.bind.annotation.PostMapping("/sugestoes")
    SugestaoPrecoResponse gerarSugestao(@RequestBody SugestaoPrecoRequest request, Authentication autenticacao) {
        return agente.gerarSugestao(request, autenticacao.getName());
    }

    @GetMapping("/sugestoes/{id}")
    SugestaoPrecoResponse buscarSugestao(@PathVariable Long id) {
        return agente.buscar(id);
    }
}
