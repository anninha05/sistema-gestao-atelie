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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Entity
@Table(name = "precificacao_execucoes_pid", uniqueConstraints =
        @UniqueConstraint(name = "uk_precificacao_execucao_dia", columnNames = { "competencia", "data_apuracao" }))
public class ExecucaoPIDDiaria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate competencia;

    @Column(name = "data_apuracao", nullable = false)
    private LocalDate dataApuracao;

    @Column(name = "delta_dias", nullable = false)
    private long deltaDias;

    @Column(name = "valor_meta", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorMeta;

    @Column(name = "resultado_realizado", nullable = false, precision = 12, scale = 2)
    private BigDecimal resultadoRealizado;

    @Column(name = "referencia_acumulada", nullable = false, precision = 12, scale = 2)
    private BigDecimal referenciaAcumulada;

    @Column(name = "erro_normalizado", nullable = false)
    private double erroNormalizado;

    @Column(name = "termo_proporcional", nullable = false)
    private double termoProporcional;

    @Column(name = "termo_integral", nullable = false)
    private double termoIntegral;

    @Column(name = "termo_derivativo", nullable = false)
    private double termoDerivativo;

    @Column(name = "saida_bruta", nullable = false)
    private double saidaBruta;

    @Column(name = "ajuste_limitado", nullable = false)
    private double ajusteLimitado;

    @Column(name = "integral_acumulada", nullable = false)
    private double integralAcumulada;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    protected ExecucaoPIDDiaria() {}

    public ExecucaoPIDDiaria(YearMonth mes, LocalDate dataApuracao, long deltaDias, BigDecimal valorMeta,
            BigDecimal resultadoRealizado, BigDecimal referenciaAcumulada, ResultadoPID resultado,
            PoliticaPrecificacao politica, Usuario responsavel) {
        if (mes == null || dataApuracao == null || !mes.equals(YearMonth.from(dataApuracao)) || deltaDias < 1
                || valorMeta == null || resultadoRealizado == null || referenciaAcumulada == null
                || resultado == null || politica == null || responsavel == null)
            throw new IllegalArgumentException("Informe dados completos e coerentes para auditar a execução PID.");
        this.competencia = mes.atDay(1);
        this.dataApuracao = dataApuracao;
        this.deltaDias = deltaDias;
        this.valorMeta = valorMeta.setScale(2, java.math.RoundingMode.HALF_UP);
        this.resultadoRealizado = resultadoRealizado.setScale(2, java.math.RoundingMode.HALF_UP);
        this.referenciaAcumulada = referenciaAcumulada.setScale(2, java.math.RoundingMode.HALF_UP);
        this.erroNormalizado = resultado.erroNormalizado();
        this.termoProporcional = resultado.termoProporcional();
        this.termoIntegral = resultado.termoIntegral();
        this.termoDerivativo = resultado.termoDerivativo();
        this.saidaBruta = resultado.saidaBruta();
        this.ajusteLimitado = resultado.ajusteLimitado();
        this.integralAcumulada = resultado.integralAcumulada();
        this.ganhoProporcional = politica.getGanhoProporcional();
        this.ganhoIntegral = politica.getGanhoIntegral();
        this.ganhoDerivativo = politica.getGanhoDerivativo();
        this.ajusteMinimo = politica.getAjusteMinimo();
        this.ajusteMaximo = politica.getAjusteMaximo();
        this.responsavel = responsavel;
    }

    public Long getId() { return id; }
    public YearMonth getMes() { return YearMonth.from(competencia); }
    public LocalDate getDataApuracao() { return dataApuracao; }
    public long getDeltaDias() { return deltaDias; }
    public BigDecimal getValorMeta() { return valorMeta; }
    public BigDecimal getResultadoRealizado() { return resultadoRealizado; }
    public BigDecimal getReferenciaAcumulada() { return referenciaAcumulada; }
    public double getErroNormalizado() { return erroNormalizado; }
    public double getTermoProporcional() { return termoProporcional; }
    public double getTermoIntegral() { return termoIntegral; }
    public double getTermoDerivativo() { return termoDerivativo; }
    public double getSaidaBruta() { return saidaBruta; }
    public double getAjusteLimitado() { return ajusteLimitado; }
    public double getIntegralAcumulada() { return integralAcumulada; }
    public double getGanhoProporcional() { return ganhoProporcional; }
    public double getGanhoIntegral() { return ganhoIntegral; }
    public double getGanhoDerivativo() { return ganhoDerivativo; }
    public double getAjusteMinimo() { return ajusteMinimo; }
    public double getAjusteMaximo() { return ajusteMaximo; }
    public Long getResponsavelId() { return responsavel.getId(); }
    public String getResponsavelNome() { return responsavel.getNome(); }
}
