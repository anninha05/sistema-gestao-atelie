package com.atelie.sistema.domain.repository;

import com.atelie.sistema.domain.model.Cliente;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepositoryGateway {

    Cliente salvar(Cliente cliente);

    Optional<Cliente> buscarPorId(UUID id);

    Optional<Cliente> buscarPorCpf(String cpf);

    boolean existePorCpf(String cpf);
}