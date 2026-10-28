package com.atelie.sistema.cliente;

import com.atelie.sistema.cliente.dto.ClienteRequestDTO;
import com.atelie.sistema.cliente.dto.ClienteResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public ClienteResponseDTO cadastrarCliente(ClienteRequestDTO dto) {
        if (dto.cpf() != null && !dto.cpf().isBlank() && clienteRepository.existsByCpf(dto.cpf())) {
            throw new IllegalArgumentException("Já existe um cliente cadastrado com este CPF.");
        }

        Cliente novoCliente = new Cliente(
                dto.nome(),
                dto.telefone(),
                dto.email(),
                dto.cpf());

        Cliente clienteSalvo = clienteRepository.save(novoCliente);

        return new ClienteResponseDTO(
                clienteSalvo.getId(),
                clienteSalvo.getNome(),
                clienteSalvo.getTelefone(),
                clienteSalvo.getEmail(),
                clienteSalvo.getCpf(),
                clienteSalvo.getCriadoEm());
    }
}