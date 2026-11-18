package com.atelie.sistema.precificacao;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoPIDRepository extends JpaRepository<EstadoPID, LocalDate> {
    Optional<EstadoPID> findByCompetencia(LocalDate competencia);
}
