package com.atelie.sistema.precificacao;

import com.atelie.sistema.precificacao.dto.MetaMensalRequest;
import com.atelie.sistema.precificacao.dto.MetaMensalResponse;
import com.atelie.sistema.precificacao.dto.PoliticaPrecificacaoRequest;
import com.atelie.sistema.precificacao.dto.PoliticaPrecificacaoResponse;
import com.atelie.sistema.precificacao.dto.ExecucaoPIDResponse;
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

    PrecificacaoController(ConfiguracaoPrecificacaoService configuracao, AtualizacaoDiariaPIDService atualizacoes) {
        this.configuracao = configuracao;
        this.atualizacoes = atualizacoes;
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
}
