package com.atelie.sistema.infrastructure.persistence;

import com.atelie.sistema.domain.model.Peca;
import com.atelie.sistema.domain.model.StatusDisponibilidade;
import com.atelie.sistema.domain.repository.PecaRepositoryGateway;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PecaRepositoryAdapter implements PecaRepositoryGateway {

    private final PecaJpaRepository jpaRepository;

    public PecaRepositoryAdapter(PecaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Peca salvar(Peca peca) {
        return jpaRepository.save(peca);
    }

    @Override
    public Optional<Peca> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Peca> buscarDisponiveis() {
        return jpaRepository.findByStatusDisponibilidade(StatusDisponibilidade.DISPONIVEL);
    }
}
