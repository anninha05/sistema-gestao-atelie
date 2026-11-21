package com.atelie.sistema.peca;

import com.atelie.sistema.peca.state.Disponivel;
import com.atelie.sistema.peca.state.EstadoPeca;
import com.atelie.sistema.peca.state.StatusDisponibilidade;
import com.atelie.sistema.shared.exception.RegraNegocioException;
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
        aplicar(new Disponivel());
    }

    public Peca(String descricao, TipoFluxo tipo, BigDecimal valorBase, String tamanho, String cor) {
        this(descricao, tipo);
        atualizarDadosDoCatalogo(descricao, valorBase == null ? BigDecimal.ZERO : valorBase, tamanho, cor);
    }

    // STATE PATTERN

    public EstadoPeca getEstado() {
        if (estado == null) {
            estado = EstadoPeca.de(statusDisponibilidade);
        }
        return estado;
    }

    private void aplicar(EstadoPeca novoEstado) {
        this.estado = novoEstado;
        this.statusDisponibilidade = novoEstado.getStatus();
    }

    @PostLoad
    protected void onPostLoad() {
        this.estado = EstadoPeca.de(this.statusDisponibilidade);
    }

    public void retirarParaLocacao() {
        if (tipo != TipoFluxo.LOCACAO || statusDisponibilidade != StatusDisponibilidade.DISPONIVEL) {
            throw new RegraNegocioException(
                    "A peça " + codigoEtiqueta + " não está pronta para retirada. Verifique sua condição no inventário.");
        }
        aplicar(getEstado().alugar(this));
    }

    public void devolverDaLocacao() {
        if (tipo != TipoFluxo.LOCACAO || statusDisponibilidade != StatusDisponibilidade.ALUGADA) {
            throw new RegraNegocioException(
                    "A peça " + codigoEtiqueta + " precisa estar alugada para registrar sua devolução.");
        }
        aplicar(getEstado().enviarParaLavagem(this));
    }

    public void enviarParaCostura() {
        aplicar(getEstado().enviarParaCostura(this));
    }

    public void enviarParaLavagem() {
        if (statusDisponibilidade == StatusDisponibilidade.ALUGADA) {
            throw new RegraNegocioException(
                    "Registre a devolução na ordem de serviço para encaminhar a peça à lavagem.");
        }
        aplicar(getEstado().enviarParaLavagem(this));
    }

    public void devolverParaEstoque(boolean lavagemConcluida, boolean condicoesConferidas) {
        if (!lavagemConcluida || !condicoesConferidas) {
            throw new RegraNegocioException(
                    "Confirme a lavagem concluída e as condições da peça antes de liberá-la ao estoque.");
        }
        aplicar(getEstado().devolverParaEstoque(this));
    }

    public void atualizarDadosDoCatalogo(String descricao, BigDecimal valorBase, String tamanho, String cor) {
        this.descricao = descricao;
        this.valorBase = valorBase;
        this.tamanho = tamanho;
        this.cor = cor;
    }

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = this.criadoEm;
    }

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCodigoEtiqueta() {
        return codigoEtiqueta;
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

    public String getTamanho() {
        return tamanho;
    }

    public String getCor() {
        return cor;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
