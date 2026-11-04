package com.atelie.sistema.ordemservico;

import com.atelie.sistema.cliente.Cliente;
import com.atelie.sistema.ordemservico.state.*;
import com.atelie.sistema.peca.TipoFluxo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    @Version
    private long versao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_fluxo", nullable = false, length = 50)
    private TipoFluxo tipoFluxo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_atual", nullable = false, length = 50)
    private StatusOrdemServico statusAtual;

    @Transient
    private EstadoOS estado;

    @Column(name = "valor_total")
    private BigDecimal valorTotal;

    @OneToOne(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private Pagamento pagamento;

    @ElementCollection
    @CollectionTable(name = "ordens_servico_pecas", joinColumns = @JoinColumn(name = "ordem_servico_id"))
    private List<ItemOrdemServico> itens = new java.util.ArrayList<>();

    private BigDecimal desconto = BigDecimal.ZERO;
    private LocalDateTime inicioLocacao;
    private LocalDateTime fimLocacao;
    private LocalDateTime dataProva;
    private LocalDateTime dataEvento;
    private String instrucoesCostura;
    private String medidas;

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
        this.setEstado(new Reservada());
        if (valorTotal != null) {
            this.pagamento = new Pagamento(this, valorTotal);
        }
    }

    public OrdemServico(Cliente cliente, TipoFluxo tipoFluxo, BigDecimal valorTotal, LocalDateTime dataEntrega,
            List<ItemOrdemServico> itens, BigDecimal desconto, LocalDateTime inicioLocacao,
            LocalDateTime fimLocacao, LocalDateTime dataProva, LocalDateTime dataEvento,
            String instrucoesCostura, String medidas) {
        this(cliente, tipoFluxo, valorTotal, dataEntrega);
        this.itens.addAll(itens);
        this.desconto = desconto;
        this.inicioLocacao = inicioLocacao;
        this.fimLocacao = fimLocacao;
        this.dataProva = dataProva;
        this.dataEvento = dataEvento;
        this.instrucoesCostura = instrucoesCostura;
        this.medidas = medidas;
    }

    public List<ItemOrdemServico> getItens() {
        return List.copyOf(itens);
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public LocalDateTime getInicioLocacao() {
        return inicioLocacao;
    }

    public LocalDateTime getFimLocacao() {
        return fimLocacao;
    }

    public LocalDateTime getDataProva() {
        return dataProva;
    }

    public LocalDateTime getDataEvento() {
        return dataEvento;
    }

    public String getInstrucoesCostura() {
        return instrucoesCostura;
    }

    public String getMedidas() {
        return medidas;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    // STATE PATTERN

    public EstadoOS getEstado() {
        if (this.estado == null) {
            this.estado = mapearEstado(this.statusAtual);
        }
        return this.estado;
    }

    public void setEstado(EstadoOS estado) {
        this.estado = estado;
        this.statusAtual = estado.getStatus();
    }

    private EstadoOS mapearEstado(StatusOrdemServico status) {
        if (status == null) {
            return new Reservada();
        }
        return switch (status) {
            case EM_PROVA, PROVA -> new EmProva();
            case RETIRADA -> new Retirada();
            case CONFECCAO, AJUSTES, PASSADORIA, CONCLUIDO, ENTREGUE, CANCELADA -> new EtapaProducao(status);
            case DEVOLVIDA -> new Devolvida();
            default -> new Reservada();
        };
    }

    @PostLoad
    protected void onPostLoad() {
        this.estado = mapearEstado(this.statusAtual);
    }

    public void iniciarConfeccao() {
        getEstado().iniciarConfeccao(this);
    }

    public void iniciarPassadoria() {
        if (statusAtual != StatusOrdemServico.AJUSTES && statusAtual != StatusOrdemServico.EM_PROVA) {
            throw new com.atelie.sistema.shared.exception.RegraNegocioException(
                    "A ordem precisa passar pelos ajustes.");
        }
        setEstado(new EtapaProducao(StatusOrdemServico.PASSADORIA));
    }

    public void cancelar() {
        switch (statusAtual) {
            case RETIRADA, ENTREGUE, DEVOLVIDA, CANCELADA ->
                throw new com.atelie.sistema.shared.exception.RegraNegocioException(
                        "A ordem não pode mais ser cancelada.");
            default -> setEstado(new EtapaProducao(StatusOrdemServico.CANCELADA));
        }
    }

    public void enviarParaProva() {
        getEstado().enviarParaProva(this);
    }

    public void concluir() {
        getEstado().concluir(this);
    }

    public void entregarAoCliente() {
        getEstado().retirar(this);
    }

    public void retirar() {
        getEstado().retirar(this);
    }

    public void devolver() {
        getEstado().devolver(this);
    }

    // PAGAMENTO (COMPOSIÇÃO)

    public void registrarSinal(BigDecimal valor, LocalDate data) {
        garantirPagamento().registrarSinal(valor, data);
    }

    public void registrarPagamento(BigDecimal valor) {
        garantirPagamento().registrarPagamento(valor);
    }

    public void alterarValorTotal(BigDecimal novoValor) {
        Pagamento pag = garantirPagamento();
        pag.alterarValorTotal(novoValor);
        this.valorTotal = pag.getValorTotal();
    }

    private Pagamento garantirPagamento() {
        if (this.pagamento == null) {
            this.pagamento = new Pagamento(this, this.valorTotal);
        }
        return this.pagamento;
    }

    // AUDITORIA

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
        if (this.statusAtual == null && this.estado != null) {
            this.statusAtual = this.estado.getStatus();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
        if (this.estado != null) {
            this.statusAtual = this.estado.getStatus();
        }
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

    public Pagamento getPagamento() {
        return garantirPagamento();
    }

    public LocalDateTime getDataEntrega() {
        return dataEntrega;
    }
}