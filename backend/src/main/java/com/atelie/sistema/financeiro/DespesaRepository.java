package com.atelie.sistema.financeiro;

import org.springframework.data.jpa.repository.JpaRepository;

interface DespesaRepository extends JpaRepository<Despesa, Long> {
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(d.valor), 0) from Despesa d where d.dataPagamento >= :inicio and d.dataPagamento < :fim")
    java.math.BigDecimal somarPagamentos(java.time.LocalDate inicio, java.time.LocalDate fim);
}
