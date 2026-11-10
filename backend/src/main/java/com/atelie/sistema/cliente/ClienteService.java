package com.atelie.sistema.cliente;

import com.atelie.sistema.cliente.dto.ClienteRequestDTO;
import com.atelie.sistema.cliente.dto.ClienteResponseDTO;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import com.atelie.sistema.shared.validacao.Email;
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
        if (dto.nome() == null || dto.nome().isBlank() || dto.nome().length() > 255) {
            throw new RegraNegocioException("Informe um nome válido.");
        }
        if (dto.telefone() == null || dto.telefone().length() > 20
                || !dto.telefone().strip().matches("(?:\\+55\\s*)?(?:[1-9][0-9]|\\([1-9][0-9]\\))\\s*[0-9]{4,5}-?[0-9]{4}")) {
            throw new RegraNegocioException("Informe um telefone com DDD e 10 ou 11 dígitos, como (22) 99999-9999.");
        }
        String telefone = dto.telefone().strip().replaceFirst("^\\+55\\s*", "")
                .replaceAll("[()\\s-]", "");
        String email = dto.email() == null ? null : dto.email().strip();
        if (!Email.valido(email) || email.length() > 100) {
            throw new RegraNegocioException("Informe um e-mail válido com até 100 caracteres.");
        }
        if (dto.cpf() == null || !dto.cpf().matches("[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}")) {
            throw new RegraNegocioException("Informe um CPF válido.");
        }
        String cpf = dto.cpf().replace(".", "").replace("-", "");
        if (cpf.chars().allMatch(digito -> digito == cpf.charAt(0))
                || digitoVerificador(cpf, 9) != cpf.charAt(9) - '0'
                || digitoVerificador(cpf, 10) != cpf.charAt(10) - '0') {
            throw new RegraNegocioException("Informe um CPF válido.");
        }
        if (clienteRepository.existsByCpf(cpf)) {
            throw new RegraNegocioException("Já existe um cliente cadastrado com este CPF.");
        }

        Cliente novoCliente = new Cliente(
                dto.nome(),
                telefone,
                email,
                cpf);

        Cliente clienteSalvo = clienteRepository.save(novoCliente);

        return toResponseDTO(clienteSalvo);
    }

    private static int digitoVerificador(String cpf, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (cpf.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
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
