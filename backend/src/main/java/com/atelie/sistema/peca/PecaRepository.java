package com.atelie.sistema.peca;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PecaRepository extends JpaRepository<Peca, UUID> {

    List<Peca> findByStatusDisponibilidade(StatusDisponibilidade status);
}