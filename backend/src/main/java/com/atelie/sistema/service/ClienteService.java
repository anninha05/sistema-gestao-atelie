package com.atelie.sistema.service;

import com.atelie.sistema.domain.model.Cliente;
import com.atelie.sistema.domain.repository.ClienteRepositoryGateway;
import com.atelie.sistema.dto.ClienteRequestDTO;
import com.atelie.sistema.dto.ClienteResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    private final ClienteRepositoryGateway clienteGateway;

    public ClienteService(ClienteRepositoryGateway clienteGateway) {
        this.clienteGateway = clienteGateway;
    }

    @Transactional
    public ClienteResponseDTO cadastrarCliente(ClienteRequestDTO dto) {
        if (dto.cpf() != null && !dto.cpf().isBlank() && clienteGateway.existePorCpf(dto.cpf())) {
            throw new IllegalArgumentException("Já existe um cliente cadastrado com este CPF.");
        }

        Cliente novoCliente = new Cliente(
                dto.nome(),
                dto.telefone(),
                dto.email(),
                dto.cpf());

        Cliente clienteSalvo = clienteGateway.salvar(novoCliente);

        return new ClienteResponseDTO(
                clienteSalvo.getId(),
                clienteSalvo.getNome(),
                clienteSalvo.getTelefone(),
                clienteSalvo.getEmail(),
                clienteSalvo.getCpf(),
                clienteSalvo.getCriadoEm());
    }
}