package com.atelie.sistema.precificacao;

import com.atelie.sistema.precificacao.PrecificacaoDTO.CalibracaoResponse;
import com.atelie.sistema.precificacao.PrecificacaoDTO.ExecucaoResponse;
import com.atelie.sistema.precificacao.PrecificacaoDTO.MetaRequest;
import com.atelie.sistema.precificacao.PrecificacaoDTO.MetaResponse;
import com.atelie.sistema.precificacao.PrecificacaoDTO.PoliticaRequest;
import com.atelie.sistema.precificacao.PrecificacaoDTO.PoliticaResponse;
import com.atelie.sistema.precificacao.PrecificacaoDTO.SugestaoRequest;
import com.atelie.sistema.precificacao.PrecificacaoDTO.SugestaoResponse;
import java.time.YearMonth;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/precificacao")
class PrecificacaoController {

    private final PrecificacaoService precificacao;
    private final SugestaoPrecoService sugestoes;

    PrecificacaoController(PrecificacaoService precificacao, SugestaoPrecoService sugestoes) {
        this.precificacao = precificacao;
        this.sugestoes = sugestoes;
    }

    @GetMapping("/metas/{mes}")
    MetaResponse buscarMeta(@PathVariable YearMonth mes) {
        return precificacao.buscarMeta(mes);
    }

    @PutMapping("/metas/{mes}")
    MetaResponse definirMeta(@PathVariable YearMonth mes, @RequestBody MetaRequest request, Authentication usuario) {
        return precificacao.definirMeta(mes, request, usuario.getName());
    }

    @GetMapping("/politica")
    PoliticaResponse buscarPolitica() {
        return precificacao.buscarPolitica();
    }

    @PutMapping("/politica")
    PoliticaResponse configurarPolitica(@RequestBody PoliticaRequest request, Authentication usuario) {
        return precificacao.configurarPolitica(request, usuario.getName());
    }

    @PostMapping("/politica/calibrar")
    CalibracaoResponse calibrarGanhos(Authentication usuario) {
        return precificacao.calibrarGanhos(usuario.getName());
    }

    @GetMapping("/atualizacoes")
    List<ExecucaoResponse> listarAtualizacoes(@RequestParam YearMonth mes) {
        return precificacao.listarExecucoes(mes);
    }

    @PostMapping("/sugestoes")
    SugestaoResponse sugerir(@RequestBody SugestaoRequest request, Authentication usuario) {
        return sugestoes.sugerir(request, usuario.getName());
    }

    @PostMapping("/sugestoes/{id}/aprovar")
    SugestaoResponse aprovar(@PathVariable Long id, Authentication usuario) {
        return sugestoes.aprovar(id, usuario.getName());
    }
}
