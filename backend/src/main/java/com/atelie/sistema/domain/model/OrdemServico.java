package com.atelie.sistema.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ordens_servico")
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_fluxo", nullable = false, length = 50)
    private TipoFluxo tipoFluxo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_atual", nullable = false, length = 50)
    private StatusOrdemServico statusAtual;

    @Column(name = "valor_total")
    private BigDecimal valorTotal;

    @Column(name = "data_entrega")
    private LocalDateTime dataEntrega;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    protected OrdemServico() {
    }

    public OrdemServico(Cliente cliente, TipoFluxo tipoFluxo, BigDecimal valorTotal, LocalDateTime dataEntrega) {
        this.cliente = cliente;
        this.tipoFluxo = tipoFluxo;
        this.valorTotal = valorTotal;
        this.dataEntrega = dataEntrega;
        this.statusAtual = StatusOrdemServico.PENDENTE;
    }

    // MÁQUINA DE ESTADOS

    public void iniciarConfeccao() {
        if (this.statusAtual != StatusOrdemServico.PENDENTE && this.statusAtual != StatusOrdemServico.AJUSTES) {
            throw new IllegalStateException("Só é possível iniciar confecção se estiver PENDENTE ou em AJUSTES.");
        }
        this.statusAtual = StatusOrdemServico.CONFECCAO;
    }

    public void enviarParaProva() {
        if (this.statusAtual != StatusOrdemServico.CONFECCAO) {
            throw new IllegalStateException("A roupa precisa estar em CONFECÇÃO para ir para PROVA.");
        }
        this.statusAtual = StatusOrdemServico.PROVA;
    }

    public void concluir() {
        if (this.statusAtual != StatusOrdemServico.PROVA && this.statusAtual != StatusOrdemServico.CONFECCAO) {
            throw new IllegalStateException("A roupa precisa ser confeccionada ou provada antes de ser CONCLUÍDA.");
        }
        this.statusAtual = StatusOrdemServico.CONCLUIDO;
    }

    public void entregarAoCliente() {
        if (this.statusAtual != StatusOrdemServico.CONCLUIDO) {
            throw new IllegalStateException("Não é possível entregar uma roupa que não está CONCLUÍDA.");
        }
        this.statusAtual = StatusOrdemServico.ENTREGUE;
    }

    // AUDITORIA

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

    public Cliente getCliente() {
        return cliente;
    }

    public TipoFluxo getTipoFluxo() {
        return tipoFluxo;
    }

    public StatusOrdemServico getStatusAtual() {
        return statusAtual;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public LocalDateTime getDataEntrega() {
        return dataEntrega;
    }
}