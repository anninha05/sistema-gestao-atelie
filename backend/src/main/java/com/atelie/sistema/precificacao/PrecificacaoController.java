package com.atelie.sistema.precificacao;

import com.atelie.sistema.precificacao.dto.MetaMensalRequest;
import com.atelie.sistema.precificacao.dto.MetaMensalResponse;
import com.atelie.sistema.precificacao.dto.PoliticaPrecificacaoRequest;
import com.atelie.sistema.precificacao.dto.PoliticaPrecificacaoResponse;
import com.atelie.sistema.precificacao.dto.CalibracaoGanhosResponse;
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
    private final SugestaoPrecoService sugestoes;

    PrecificacaoController(ConfiguracaoPrecificacaoService configuracao, AtualizacaoDiariaPIDService atualizacoes,
            SugestaoPrecoService sugestoes) {
        this.configuracao = configuracao;
        this.atualizacoes = atualizacoes;
        this.sugestoes = sugestoes;
    }

    @GetMapping("/metas/{mes}")
    MetaMensalResponse buscarMeta(@PathVariable YearMonth mes) {
        return configuracao.buscarMeta(mes);
    }

    @PutMapping("/metas/{mes}")
    MetaMensalResponse definirMeta(@PathVariable YearMonth mes, @RequestBody MetaMensalRequest request,
            Authentication autenticacao) {
        var meta = configuracao.definirMeta(mes, request, autenticacao.getName());
        if (mes.equals(YearMonth.now())) atualizacoes.atualizarAutomaticamente();
        return meta;
    }

    @GetMapping("/ordens/{id}/preco-minimo")
    java.util.Map<String, java.math.BigDecimal> buscarPiso(@PathVariable java.util.UUID id) {
        return java.util.Map.of("valor", configuracao.buscarPiso(id));
    }

    @PutMapping("/ordens/{id}/preco-minimo")
    java.util.Map<String, java.math.BigDecimal> salvarPiso(@PathVariable java.util.UUID id,
            @RequestBody com.atelie.sistema.precificacao.dto.PisoPrecoRequest request, Authentication autenticacao) {
        return java.util.Map.of("valor", configuracao.salvarPiso(id, request.valor(), autenticacao.getName()));
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

    @PostMapping("/politica/calibrar")
    CalibracaoGanhosResponse calibrarGanhos(Authentication autenticacao) {
        return configuracao.calibrarGanhos(autenticacao.getName());
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
        atualizacoes.atualizarAutomaticamente();
        return sugestoes.simular(request, autenticacao.getName());
    }

    @org.springframework.web.bind.annotation.PostMapping("/sugestoes")
    SugestaoPrecoResponse gerarSugestao(@RequestBody SugestaoPrecoRequest request, Authentication autenticacao) {
        atualizacoes.atualizarAutomaticamente();
        return sugestoes.gerarSugestao(request, autenticacao.getName());
    }

    @GetMapping("/sugestoes/{id}")
    SugestaoPrecoResponse buscarSugestao(@PathVariable Long id) {
        return sugestoes.buscar(id);
    }
}
