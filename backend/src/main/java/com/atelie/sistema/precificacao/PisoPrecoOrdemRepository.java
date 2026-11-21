package com.atelie.sistema.precificacao;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PisoPrecoOrdemRepository extends JpaRepository<PisoPrecoOrdem, UUID> {}
