package com.atelie.sistema.financeiro;

import com.atelie.sistema.ordemservico.MetodoPagamento;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DespesaService {
    private final DespesaRepository despesas;

    DespesaService(DespesaRepository despesas) {
        this.despesas = despesas;
    }

    @Transactional
    public Despesa cadastrar(String descricao, String categoria, BigDecimal valor, LocalDate vencimento) {
        if (descricao == null || descricao.isBlank() || descricao.length() > 255
                || categoria == null || categoria.isBlank() || categoria.length() > 100
                || valor == null || valor.signum() <= 0 || valor.stripTrailingZeros().scale() > 2
                || valor.compareTo(new BigDecimal("99999999.99")) > 0 || vencimento == null) {
            throw new RegraNegocioException("Informe os dados válidos da despesa.");
        }
        return despesas.save(new Despesa(descricao, categoria, valor, vencimento));
    }

    public List<Despesa> listar() {
        return despesas.findAll();
    }

    @Transactional
    public Despesa baixar(Long id, LocalDate data, MetodoPagamento metodo) {
        Despesa despesa = despesas.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa não encontrada: " + id));
        if (data == null || metodo == null || despesa.getDataPagamento() != null) {
            throw new RegraNegocioException("Informe a data e o método de uma despesa em aberto.");
        }
        despesa.baixar(data, metodo);
        return despesa;
    }
}
