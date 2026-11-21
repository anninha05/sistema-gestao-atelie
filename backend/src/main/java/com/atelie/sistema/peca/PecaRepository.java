package com.atelie.sistema.peca;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PecaRepository extends JpaRepository<Peca, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Peca p where p.id = :id")
    Optional<Peca> buscarParaReserva(UUID id);
}
