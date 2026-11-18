package com.atelie.sistema.precificacao;

import java.time.YearMonth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface MetaMensalRepository extends JpaRepository<MetaMensal, Long> {
    @Query("select m from MetaMensal m where m.competencia = :competencia")
    Optional<MetaMensal> buscarPorMes(@Param("competencia") java.time.LocalDate competencia);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MetaMensal m where m.competencia = :competencia")
    Optional<MetaMensal> bloquearPorMes(@Param("competencia") java.time.LocalDate competencia);
}
