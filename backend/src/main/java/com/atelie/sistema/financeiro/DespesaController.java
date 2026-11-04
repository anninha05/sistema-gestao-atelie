package com.atelie.sistema.financeiro;

import com.atelie.sistema.ordemservico.MetodoPagamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/financeiro/despesas")
class DespesaController {
    private final DespesaService despesas;

    DespesaController(DespesaService despesas) {
        this.despesas = despesas;
    }

    record Cadastro(String descricao, String categoria, BigDecimal valor, LocalDate vencimento) {}
    record Baixa(LocalDate data, MetodoPagamento metodo) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Despesa cadastrar(@RequestBody Cadastro dto) {
        return despesas.cadastrar(dto.descricao(), dto.categoria(), dto.valor(), dto.vencimento());
    }

    @GetMapping
    List<Despesa> listar() {
        return despesas.listar();
    }

    @PatchMapping("/{id}/baixa")
    Despesa baixar(@PathVariable Long id, @RequestBody Baixa dto) {
        return despesas.baixar(id, dto.data(), dto.metodo());
    }
}
