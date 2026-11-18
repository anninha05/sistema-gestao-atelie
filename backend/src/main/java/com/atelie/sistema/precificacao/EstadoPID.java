package com.atelie.sistema.precificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.time.LocalDate;
import java.time.YearMonth;

@Entity
@Table(name = "precificacao_estados_pid")
public class EstadoPID {
    @Id
    @Column(name = "competencia", nullable = false)
    private LocalDate competencia;

    @Column(name = "integral_acumulada", nullable = false)
    private double integralAcumulada;

    @Column(name = "erro_anterior")
    private Double erroAnterior;

    @Column(name = "ultima_atualizacao")
    private LocalDate ultimaAtualizacao;

    protected EstadoPID() {}

    public EstadoPID(YearMonth mes) {
        if (mes == null) throw new RegraNegocioException("Informe a competência do estado PID.");
        this.competencia = mes.atDay(1);
    }

    public void registrar(double integralAcumulada, Double erroAnterior, LocalDate data) {
        if (!Double.isFinite(integralAcumulada) || erroAnterior != null && !Double.isFinite(erroAnterior))
            throw new RegraNegocioException("O estado PID deve conter valores finitos.");
        if (data == null || !YearMonth.from(data).equals(getMes()))
            throw new RegraNegocioException("A atualização deve pertencer à competência do estado PID.");
        if (ultimaAtualizacao != null && !data.isAfter(ultimaAtualizacao))
            throw new RegraNegocioException("A data da atualização deve ser posterior à última atualização.");
        this.integralAcumulada = integralAcumulada;
        this.erroAnterior = erroAnterior;
        this.ultimaAtualizacao = data;
    }

    public YearMonth getMes() { return YearMonth.from(competencia); }
    public double getIntegralAcumulada() { return integralAcumulada; }
    public Double getErroAnterior() { return erroAnterior; }
    public LocalDate getUltimaAtualizacao() { return ultimaAtualizacao; }
}
