package com.atelie.sistema.precificacao;

import com.atelie.sistema.usuario.Usuario;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "precificacao_politica")
public class PoliticaPrecificacao {
    public static final long ID_UNICA = 1L;

    @Id
    private Long id = ID_UNICA;

    @Column(name = "ganho_proporcional", nullable = false)
    private double ganhoProporcional;

    @Column(name = "ganho_integral", nullable = false)
    private double ganhoIntegral;

    @Column(name = "ganho_derivativo", nullable = false)
    private double ganhoDerivativo;

    @Column(name = "ajuste_minimo", nullable = false)
    private double ajusteMinimo;

    @Column(name = "ajuste_maximo", nullable = false)
    private double ajusteMaximo;

    @Column(name = "desconto_fidelidade", precision = 5, scale = 4)
    private BigDecimal descontoFidelidade;

    @Column(name = "quantidade_os_fidelidade")
    private Integer quantidadeOsFidelidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    protected PoliticaPrecificacao() {}

    public PoliticaPrecificacao(double kp, double ki, double kd, double minimo, double maximo,
            BigDecimal descontoFidelidade, Integer quantidadeOsFidelidade, Usuario responsavel) {
        this.id = ID_UNICA;
        configurar(kp, ki, kd, minimo, maximo, descontoFidelidade, quantidadeOsFidelidade, responsavel);
    }

    public void configurar(double kp, double ki, double kd, double minimo, double maximo,
            BigDecimal descontoFidelidade, Integer quantidadeOsFidelidade, Usuario responsavel) {
        if (!Double.isFinite(kp) || !Double.isFinite(ki) || !Double.isFinite(kd)
                || !Double.isFinite(minimo) || !Double.isFinite(maximo) || minimo >= maximo)
            throw new RegraNegocioException("Informe ganhos PID finitos e limites de ajuste válidos.");
        boolean fidelidadeConfigurada = descontoFidelidade != null || quantidadeOsFidelidade != null;
        if (fidelidadeConfigurada && (descontoFidelidade == null || quantidadeOsFidelidade == null
                || descontoFidelidade.signum() < 0 || descontoFidelidade.compareTo(BigDecimal.ONE) >= 0
                || quantidadeOsFidelidade < 1))
            throw new RegraNegocioException("Configure juntos um desconto de fidelidade válido e a quantidade mínima de ordens.");
        if (responsavel == null) throw new RegraNegocioException("Informe quem configurou a política.");
        this.ganhoProporcional = kp;
        this.ganhoIntegral = ki;
        this.ganhoDerivativo = kd;
        this.ajusteMinimo = minimo;
        this.ajusteMaximo = maximo;
        this.descontoFidelidade = descontoFidelidade == null ? null
                : descontoFidelidade.setScale(4, java.math.RoundingMode.HALF_UP);
        this.quantidadeOsFidelidade = quantidadeOsFidelidade;
        this.responsavel = responsavel;
        this.atualizadaEm = LocalDateTime.now();
    }

    public ControladorPID.Ganhos getGanhos() {
        return new ControladorPID.Ganhos(ganhoProporcional, ganhoIntegral, ganhoDerivativo, ajusteMinimo, ajusteMaximo);
    }

    public Long getId() { return id; }
    public double getGanhoProporcional() { return ganhoProporcional; }
    public double getGanhoIntegral() { return ganhoIntegral; }
    public double getGanhoDerivativo() { return ganhoDerivativo; }
    public double getAjusteMinimo() { return ajusteMinimo; }
    public double getAjusteMaximo() { return ajusteMaximo; }
    public BigDecimal getDescontoFidelidade() { return descontoFidelidade; }
    public Integer getQuantidadeOsFidelidade() { return quantidadeOsFidelidade; }
    public Usuario getResponsavel() { return responsavel; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
}
