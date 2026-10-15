package com.atelie.sistema.infrastructure.persistence;

import com.atelie.sistema.domain.model.Peca;
import com.atelie.sistema.domain.model.StatusDisponibilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PecaJpaRepository extends JpaRepository<Peca, UUID> {

    List<Peca> findByStatusDisponibilidade(StatusDisponibilidade status);
}