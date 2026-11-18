package com.atelie.sistema.precificacao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecucaoPIDDiariaRepository extends JpaRepository<ExecucaoPIDDiaria, Long> {
    Optional<ExecucaoPIDDiaria> findByCompetenciaAndDataApuracao(LocalDate competencia, LocalDate dataApuracao);
    List<ExecucaoPIDDiaria> findByCompetenciaOrderByDataApuracaoDesc(LocalDate competencia);
}
