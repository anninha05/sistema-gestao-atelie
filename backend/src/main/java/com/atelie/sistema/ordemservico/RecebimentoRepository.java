package com.atelie.sistema.ordemservico;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecebimentoRepository extends JpaRepository<Recebimento, Long> {
    @Query("select coalesce(sum(r.valor), 0) from Recebimento r where r.data >= :inicio and r.data < :fim")
    BigDecimal somarRecebimentos(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
