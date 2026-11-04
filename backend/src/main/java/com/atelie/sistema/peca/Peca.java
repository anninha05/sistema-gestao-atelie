package com.atelie.sistema.peca;

import com.atelie.sistema.peca.state.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pecas")
public class Peca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo_etiqueta", nullable = false, unique = true, length = 40)
    private String codigoEtiqueta = UUID.randomUUID().toString();

    public String getCodigoEtiqueta() {
        return codigoEtiqueta;
    }

    @Column(nullable = false)
    private String descricao;

    @Column(name = "valor_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorBase = BigDecimal.ZERO;

    @Column(name = "tamanho")
    private String tamanho;

    @Column(name = "cor")
    private String cor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_peca", nullable = false, length = 50)
    private TipoFluxo tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_atual", nullable = false, length = 50)
    private StatusDisponibilidade statusDisponibilidade;

    @Transient
    private EstadoPeca estado;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    protected Peca() {
    }

    public Peca(String descricao, TipoFluxo tipo) {
        this.descricao = descricao;
        this.tipo = tipo;
        this.setEstado(new Disponivel());
    }

    // STATE PATTERN

    public EstadoPeca getEstado() {
        if (this.estado == null) {
            this.estado = mapearEstado(this.statusDisponibilidade);
        }
        return this.estado;
    }

    public void setEstado(EstadoPeca estado) {
        this.estado = estado;
        this.statusDisponibilidade = estado.getStatus();
    }

    private EstadoPeca mapearEstado(StatusDisponibilidade status) {
        if (status == null) {
            return new Disponivel();
        }
        return switch (status) {
            case ALUGADA -> new Alugada();
            case EM_COSTURA, MANUTENCAO -> new EmCostura();
            case EM_LAVAGEM -> new EmLavagem();
            case DISPONIVEL -> new Disponivel();
        };
    }

    @PostLoad
    protected void onPostLoad() {
        this.estado = mapearEstado(this.statusDisponibilidade);
    }

    public void marcarComoAlugada() {
        getEstado().alugar(this);
    }

    public void enviarParaCostura() {
        getEstado().enviarParaCostura(this);
    }

    public void enviarParaLavagem() {
        getEstado().enviarParaLavagem(this);
    }

    public void devolverParaEstoque() {
        getEstado().devolverParaEstoque(this);
    }

    public void enviarParaManutencao() {
        enviarParaCostura();
    }

    // AUDITORIA

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
        if (this.statusDisponibilidade == null && this.estado != null) {
            this.statusDisponibilidade = this.estado.getStatus();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
        if (this.estado != null) {
            this.statusDisponibilidade = this.estado.getStatus();
        }
    }

    // GETTERS & SETTERS

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

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public void setValorBase(BigDecimal valorBase) {
        this.valorBase = valorBase;
    }

    public String getTamanho() {
        return tamanho;
    }

    public void setTamanho(String tamanho) {
        this.tamanho = tamanho;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}