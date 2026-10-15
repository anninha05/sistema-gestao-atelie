package com.atelie.sistema.infrastructure.persistence;

import com.atelie.sistema.domain.model.Cliente;
import com.atelie.sistema.domain.repository.ClienteRepositoryGateway;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ClienteRepositoryAdapter implements ClienteRepositoryGateway {

    private final ClienteJpaRepository jpaRepository;

    public ClienteRepositoryAdapter(ClienteJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Cliente salvar(Cliente cliente) {
        return jpaRepository.save(cliente);
    }

    @Override
    public Optional<Cliente> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Cliente> buscarPorCpf(String cpf) {
        return jpaRepository.findByCpf(cpf);
    }

    @Override
    public boolean existePorCpf(String cpf) {
        return jpaRepository.existsByCpf(cpf);
    }
}
