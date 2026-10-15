package com.atelie.sistema.domain.repository;

import com.atelie.sistema.domain.model.Peca;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface PecaRepositoryGateway {
    Peca salvar(Peca peca);

    Optional<Peca> buscarPorId(UUID id);

    List<Peca> buscarDisponiveis();
}