package com.atelie.sistema.precificacao;

import com.atelie.sistema.usuario.Usuario;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Entity
@Table(name = "precificacao_metas_mensais", uniqueConstraints =
        @UniqueConstraint(name = "uk_precificacao_meta_competencia", columnNames = "competencia"))
public class MetaMensal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate competencia;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorMeta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private IndicadorPrecificacao indicador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    protected MetaMensal() {}

    public MetaMensal(YearMonth mes, BigDecimal valorMeta, IndicadorPrecificacao indicador, Usuario responsavel) {
        if (mes == null) throw new RegraNegocioException("Informe a competência da meta.");
        this.competencia = mes.atDay(1);
        alterar(valorMeta, indicador, responsavel);
    }

    public void alterar(BigDecimal valorMeta, IndicadorPrecificacao indicador, Usuario responsavel) {
        if (valorMeta == null || valorMeta.signum() <= 0)
            throw new RegraNegocioException("A meta mensal deve ser maior que zero.");
        if (indicador == null) throw new RegraNegocioException("Informe o indicador da meta.");
        if (responsavel == null) throw new RegraNegocioException("Informe quem definiu a meta.");
        this.valorMeta = valorMeta.setScale(2, java.math.RoundingMode.HALF_UP);
        this.indicador = indicador;
        this.responsavel = responsavel;
    }

    public Long getId() { return id; }
    public YearMonth getMes() { return YearMonth.from(competencia); }
    public BigDecimal getValorMeta() { return valorMeta; }
    public IndicadorPrecificacao getIndicador() { return indicador; }
    public Usuario getResponsavel() { return responsavel; }
}
