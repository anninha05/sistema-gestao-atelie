package com.atelie.sistema.precificacao;

import com.atelie.sistema.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "precificacao_aprovacoes")
public class AprovacaoPreco {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ordem_servico_id", nullable = false)
    private UUID ordemServicoId;

    @Column(name = "sugestao_id", nullable = false, unique = true)
    private Long sugestaoId;

    @Column(name = "valor_anterior", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorAnterior;

    @Column(name = "valor_aprovado", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorAprovado;

    @Column(name = "valor_recebido", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorRecebido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    @Column(name = "aprovada_em", nullable = false)
    private LocalDateTime aprovadaEm;

    protected AprovacaoPreco() {}

    public AprovacaoPreco(UUID ordemServicoId, Long sugestaoId, BigDecimal valorAnterior,
            BigDecimal valorAprovado, BigDecimal valorRecebido, Usuario responsavel) {
        this.ordemServicoId = ordemServicoId;
        this.sugestaoId = sugestaoId;
        this.valorAnterior = valorAnterior;
        this.valorAprovado = valorAprovado;
        this.valorRecebido = valorRecebido;
        this.responsavel = responsavel;
        this.aprovadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UUID getOrdemServicoId() { return ordemServicoId; }
    public Long getSugestaoId() { return sugestaoId; }
    public BigDecimal getValorAnterior() { return valorAnterior; }
    public BigDecimal getValorAprovado() { return valorAprovado; }
    public BigDecimal getValorRecebido() { return valorRecebido; }
    public Usuario getResponsavel() { return responsavel; }
    public LocalDateTime getAprovadaEm() { return aprovadaEm; }
}
