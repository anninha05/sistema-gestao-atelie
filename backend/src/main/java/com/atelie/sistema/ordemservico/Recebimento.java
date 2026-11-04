package com.atelie.sistema.ordemservico;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recebimentos")
public class Recebimento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pagamento_id", nullable = false)
    private Pagamento pagamento;

    private BigDecimal valor;
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    private MetodoPagamento metodo;

    private boolean sinal;

    protected Recebimento() {
    }

    Recebimento(Pagamento pagamento, BigDecimal valor, LocalDate data, MetodoPagamento metodo, boolean sinal) {
        this.pagamento = pagamento;
        this.valor = valor;
        this.data = data;
        this.metodo = metodo;
        this.sinal = sinal;
    }

    public BigDecimal getValor() { return valor; }
    public LocalDate getData() { return data; }
    public MetodoPagamento getMetodo() { return metodo; }
    public boolean isSinal() { return sinal; }
}
