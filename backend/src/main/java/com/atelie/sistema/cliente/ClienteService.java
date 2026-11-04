package com.atelie.sistema.cliente;

import com.atelie.sistema.cliente.dto.ClienteRequestDTO;
import com.atelie.sistema.cliente.dto.ClienteResponseDTO;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public ClienteResponseDTO cadastrarCliente(ClienteRequestDTO dto) {
        if (dto.nome() == null || dto.nome().isBlank() || dto.nome().length() > 255
                || dto.cpf() == null || !dto.cpf().matches("[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}")
                || dto.telefone() == null || dto.telefone().isBlank() || dto.telefone().length() > 20
                || dto.email() == null || !dto.email().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || dto.email().length() > 100) {
            throw new RegraNegocioException("Informe nome, CPF, telefone e e-mail válidos.");
        }
        String cpf = dto.cpf().replace(".", "").replace("-", "");
        if (clienteRepository.existsByCpf(cpf)) {
            throw new RegraNegocioException("Já existe um cliente cadastrado com este CPF.");
        }

        Cliente novoCliente = new Cliente(
                dto.nome(),
                dto.telefone(),
                dto.email(),
                cpf);

        Cliente clienteSalvo = clienteRepository.save(novoCliente);

        return toResponseDTO(clienteSalvo);
    }

    public List<ClienteResponseDTO> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ClienteResponseDTO buscarPorId(UUID id) {
        return toResponseDTO(buscarEntidadePorId(id));
    }

    public Cliente buscarEntidadePorId(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com id: " + id));
    }

    private ClienteResponseDTO toResponseDTO(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getNome(),
                cliente.getTelefone(),
                cliente.getEmail(),
                cliente.getCpf(),
                cliente.getCriadoEm());
    }
}