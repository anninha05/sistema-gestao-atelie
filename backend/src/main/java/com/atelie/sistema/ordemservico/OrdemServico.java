package com.atelie.sistema.ordemservico;

import com.atelie.sistema.cliente.Cliente;
import com.atelie.sistema.ordemservico.state.AcaoDisponivel;
import com.atelie.sistema.ordemservico.state.EstadoOS;
import com.atelie.sistema.ordemservico.state.Reservada;
import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.peca.TipoFluxo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private List<ItemOrdemServico> itens = new ArrayList<>();

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

    @Column(name = "pendencia_acerto", nullable = false)
    private boolean pendenciaAcerto;

    protected OrdemServico() {
    }

    public OrdemServico(Cliente cliente, TipoFluxo tipoFluxo, BigDecimal valorTotal, LocalDateTime dataEntrega) {
        this.cliente = cliente;
        this.tipoFluxo = tipoFluxo;
        this.valorTotal = valorTotal;
        this.dataEntrega = dataEntrega;
        aplicar(new Reservada());
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
        if (estado == null) {
            estado = EstadoOS.de(statusAtual);
        }
        return estado;
    }

    private void aplicar(EstadoOS novoEstado) {
        this.estado = novoEstado;
        this.statusAtual = novoEstado.getStatus();
    }

    @PostLoad
    protected void onPostLoad() {
        this.estado = EstadoOS.de(this.statusAtual);
    }

    public List<AcaoDisponivel> acoesDisponiveis() {
        return getEstado().acoesDisponiveis(this);
    }

    public boolean estaEmProducao() {
        return getEstado().estaEmProducao();
    }

    public boolean estaQuitada() {
        return getPagamento().getSaldoDevedor().signum() == 0;
    }

    public void iniciarConfeccao() {
        aplicar(getEstado().iniciarConfeccao(this));
    }

    public void enviarParaProva() {
        aplicar(getEstado().enviarParaProva(this));
    }

    public void iniciarPassadoria() {
        aplicar(getEstado().iniciarPassadoria(this));
    }

    public void concluir() {
        aplicar(getEstado().concluir(this));
    }

    public void retirar() {
        aplicar(getEstado().retirar(this));
    }

    public void devolver() {
        aplicar(getEstado().devolver(this));
    }

    public void cancelar() {
        aplicar(getEstado().cancelar(this));
        pendenciaAcerto = getPagamento().getValorPago().signum() > 0;
    }

    public void reagendar(LocalDateTime inicioLocacao, LocalDateTime fimLocacao,
            LocalDateTime dataProva, LocalDateTime dataEvento, LocalDateTime dataEntrega) {
        getEstado().validarReagendamento();
        boolean locacao = tipoFluxo == TipoFluxo.LOCACAO;
        this.inicioLocacao = locacao ? inicioLocacao : null;
        this.fimLocacao = locacao ? fimLocacao : null;
        this.dataProva = dataProva;
        this.dataEvento = dataEvento;
        this.dataEntrega = dataEntrega;
    }

    // PAGAMENTO (COMPOSIÇÃO)

    public void registrarRecebimento(BigDecimal valor, LocalDate data, MetodoPagamento metodo, boolean sinal) {
        getEstado().validarRecebimento();
        getPagamento().registrarRecebimento(valor, data, metodo, sinal);
    }

    public void alterarValorTotal(BigDecimal novoValor) {
        Pagamento pag = getPagamento();
        pag.alterarValorTotal(novoValor);
        this.valorTotal = pag.getValorTotal();
    }

    // AUDITORIA

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
        if (pagamento == null) {
            pagamento = new Pagamento(this, valorTotal);
        }
        return pagamento;
    }

    public LocalDateTime getDataEntrega() {
        return dataEntrega;
    }

    public boolean isPendenciaAcerto() {
        return pendenciaAcerto;
    }
}
