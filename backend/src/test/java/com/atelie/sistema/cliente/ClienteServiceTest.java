package com.atelie.sistema.cliente;

import com.atelie.sistema.cliente.dto.ClienteRequestDTO;
import com.atelie.sistema.cliente.dto.ClienteResponseDTO;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    private ClienteService clienteService;

    @BeforeEach
    void setUp() {
        clienteService = new ClienteService(clienteRepository);
    }

    @Test
    @DisplayName("Deve cadastrar cliente com sucesso quando CPF não existir")
    void deveCadastrarClienteComSucesso() {
        ClienteRequestDTO dto = new ClienteRequestDTO("Maria", "22999999999", "maria@email.com", "12345678900");
        Cliente clienteSalvo = new Cliente("Maria", "22999999999", "maria@email.com", "12345678900");

        when(clienteRepository.existsByCpf("12345678900")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteSalvo);

        ClienteResponseDTO response = clienteService.cadastrarCliente(dto);

        assertNotNull(response);
        assertEquals("Maria", response.nome());
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException quando CPF for duplicado")
    void deveLancarExcecaoQuandoCpfDuplicado() {
        ClienteRequestDTO dto = new ClienteRequestDTO("Maria", "22999999999", "maria@email.com", "12345678900");

        when(clienteRepository.existsByCpf("12345678900")).thenReturn(true);

        assertThrows(RegraNegocioException.class, () -> clienteService.cadastrarCliente(dto));
        verify(clienteRepository, never()).save(any(Cliente.class));
    }
}
