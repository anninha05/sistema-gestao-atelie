package com.atelie.sistema.precificacao;

import com.atelie.sistema.shared.exception.RegraNegocioException;
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
@Table(name = "precificacao_sugestoes")
public class SugestaoPreco {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ordem_servico_id", nullable = false)
    private UUID ordemServicoId;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "execucao_pid_id")
    private Long execucaoPidId;

    @Column(name = "preco_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoBase;

    @Column(name = "ajuste_pid", nullable = false)
    private double ajustePid;

    @Column(name = "preco_ajustado", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoAjustado;

    @Column(name = "desconto_fidelidade", nullable = false, precision = 5, scale = 4)
    private BigDecimal descontoFidelidade;

    @Column(name = "preco_apos_fidelidade", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoAposFidelidade;

    @Column(name = "piso_minimo", nullable = false, precision = 12, scale = 2)
    private BigDecimal pisoMinimo;

    @Column(name = "preco_sugerido", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoSugerido;

    @Column(name = "ordens_concluidas", nullable = false)
    private int ordensConcluidas;

    @Column(name = "ordens_canceladas", nullable = false)
    private int ordensCanceladas;

    @Column(name = "elegivel_fidelidade", nullable = false)
    private boolean elegivelFidelidade;

    @Column(nullable = false, length = 2000)
    private String justificativa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovada_por_id")
    private Usuario aprovadaPor;

    @Column(name = "aprovada_em")
    private LocalDateTime aprovadaEm;

    protected SugestaoPreco() {}

    public SugestaoPreco(UUID ordemServicoId, UUID clienteId, Long execucaoPidId,
            CalculadoraSugestaoPreco.Calculo calculo, Usuario responsavel) {
        if (ordemServicoId == null || clienteId == null || calculo == null || responsavel == null)
            throw new IllegalArgumentException("Informe a ordem, o cliente, o cálculo e quem gerou a sugestão.");
        this.ordemServicoId = ordemServicoId;
        this.clienteId = clienteId;
        this.execucaoPidId = execucaoPidId;
        this.precoBase = calculo.precoBase();
        this.ajustePid = calculo.ajustePid();
        this.precoAjustado = calculo.precoAjustado();
        this.descontoFidelidade = calculo.descontoFidelidade();
        this.precoAposFidelidade = calculo.precoAposFidelidade();
        this.pisoMinimo = calculo.pisoMinimo();
        this.precoSugerido = calculo.precoSugerido();
        this.ordensConcluidas = calculo.ordensConcluidas();
        this.ordensCanceladas = calculo.ordensCanceladas();
        this.elegivelFidelidade = calculo.elegivelFidelidade();
        this.justificativa = calculo.justificativa();
        this.responsavel = responsavel;
        this.criadaEm = LocalDateTime.now();
    }

    public void aprovar(Usuario gerente) {
        if (gerente == null) throw new RegraNegocioException("Informe quem aprovou a sugestão.");
        if (aprovadaEm != null) throw new RegraNegocioException("Esta sugestão já foi aprovada.");
        this.aprovadaPor = gerente;
        this.aprovadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UUID getOrdemServicoId() { return ordemServicoId; }
    public UUID getClienteId() { return clienteId; }
    public Long getExecucaoPidId() { return execucaoPidId; }
    public BigDecimal getPrecoBase() { return precoBase; }
    public double getAjustePid() { return ajustePid; }
    public BigDecimal getPrecoAjustado() { return precoAjustado; }
    public BigDecimal getDescontoFidelidade() { return descontoFidelidade; }
    public BigDecimal getPrecoAposFidelidade() { return precoAposFidelidade; }
    public BigDecimal getPisoMinimo() { return pisoMinimo; }
    public BigDecimal getPrecoSugerido() { return precoSugerido; }
    public int getOrdensConcluidas() { return ordensConcluidas; }
    public int getOrdensCanceladas() { return ordensCanceladas; }
    public boolean isElegivelFidelidade() { return elegivelFidelidade; }
    public String getJustificativa() { return justificativa; }
    public Usuario getResponsavel() { return responsavel; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public Usuario getAprovadaPor() { return aprovadaPor; }
    public LocalDateTime getAprovadaEm() { return aprovadaEm; }
}
