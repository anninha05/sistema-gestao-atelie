package com.atelie.sistema.financeiro;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "despesas")
public class Despesa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private long versao;

    private String descricao;
    private String categoria;
    private BigDecimal valor;
    private LocalDate vencimento;
    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    private com.atelie.sistema.ordemservico.MetodoPagamento metodo;

    protected Despesa() {
    }

    Despesa(String descricao, String categoria, BigDecimal valor, LocalDate vencimento) {
        this.descricao = descricao;
        this.categoria = categoria;
        this.valor = valor;
        this.vencimento = vencimento;
    }

    void baixar(LocalDate data, com.atelie.sistema.ordemservico.MetodoPagamento metodo) {
        this.dataPagamento = data;
        this.metodo = metodo;
    }

    public Long getId() { return id; }
    public String getDescricao() { return descricao; }
    public String getCategoria() { return categoria; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getVencimento() { return vencimento; }
    public LocalDate getDataPagamento() { return dataPagamento; }
    public com.atelie.sistema.ordemservico.MetodoPagamento getMetodo() { return metodo; }
}
