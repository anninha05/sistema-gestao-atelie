package com.atelie.sistema.domain.model;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "pecas")
public class Peca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "valor_base", nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal valorBase = java.math.BigDecimal.ZERO;

    @Column(name = "tamanho")
    private String tamanho;

    @Column(name = "cor")
    private String cor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_peca", nullable = false, length = 50)
    private TipoFluxo tipo;

    public void setValorBase(java.math.BigDecimal valorBase) {
        this.valorBase = valorBase;
    }

    public void setTamanho(String tamanho) {
        this.tamanho = tamanho;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_atual", nullable = false, length = 50)
    private StatusDisponibilidade statusDisponibilidade;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    protected Peca() {
    }

    public Peca(String descricao, TipoFluxo tipo) {
        this.descricao = descricao;
        this.tipo = tipo;
        this.statusDisponibilidade = StatusDisponibilidade.DISPONIVEL;
    }

    public void marcarComoAlugada() {
        if (this.statusDisponibilidade != StatusDisponibilidade.DISPONIVEL) {
            throw new IllegalStateException("A peça não está disponível para locação.");
        }
        this.statusDisponibilidade = StatusDisponibilidade.ALUGADA;
    }

    public void enviarParaManutencao() {
        this.statusDisponibilidade = StatusDisponibilidade.MANUTENCAO;
    }

    public void devolverParaEstoque() {
        this.statusDisponibilidade = StatusDisponibilidade.DISPONIVEL;
    }

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public TipoFluxo getTipo() {
        return tipo;
    }

    public StatusDisponibilidade getStatusDisponibilidade() {
        return statusDisponibilidade;
    }
}