package com.atelie.sistema.precificacao;

import com.atelie.sistema.precificacao.dto.MetaMensalRequest;
import com.atelie.sistema.precificacao.dto.MetaMensalResponse;
import com.atelie.sistema.precificacao.dto.PoliticaPrecificacaoRequest;
import com.atelie.sistema.precificacao.dto.PoliticaPrecificacaoResponse;
import java.time.YearMonth;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/precificacao")
class PrecificacaoController {
    private final ConfiguracaoPrecificacaoService configuracao;

    PrecificacaoController(ConfiguracaoPrecificacaoService configuracao) {
        this.configuracao = configuracao;
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
}
