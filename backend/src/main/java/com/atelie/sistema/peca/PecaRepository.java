package com.atelie.sistema.peca;

import com.atelie.sistema.peca.state.StatusDisponibilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PecaRepository extends JpaRepository<Peca, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Peca p where p.id = :id")
    java.util.Optional<Peca> buscarParaReserva(UUID id);

    List<Peca> findByStatusDisponibilidade(StatusDisponibilidade status);
}