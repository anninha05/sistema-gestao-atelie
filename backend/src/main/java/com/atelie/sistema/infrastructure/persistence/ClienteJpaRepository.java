package com.atelie.sistema.infrastructure.persistence;

import com.atelie.sistema.domain.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ClienteJpaRepository extends JpaRepository<Cliente, UUID> {

    Optional<Cliente> findByCpf(String cpf);

    boolean existsByCpf(String cpf);
}