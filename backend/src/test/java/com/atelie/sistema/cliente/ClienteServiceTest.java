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
        ClienteRequestDTO dto = new ClienteRequestDTO("Maria", "22999999999", "maria@email.com", "529.982.247-25");
        Cliente clienteSalvo = new Cliente("Maria", "22999999999", "maria@email.com", "52998224725");

        when(clienteRepository.existsByCpf("52998224725")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteSalvo);

        ClienteResponseDTO response = clienteService.cadastrarCliente(dto);

        assertNotNull(response);
        assertEquals("Maria", response.nome());
        verify(clienteRepository).save(argThat(cliente -> cliente.getCpf().equals("52998224725")));
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException quando CPF for duplicado")
    void deveLancarExcecaoQuandoCpfDuplicado() {
        ClienteRequestDTO dto = new ClienteRequestDTO("Maria", "22999999999", "maria@email.com", "52998224725");

        when(clienteRepository.existsByCpf("52998224725")).thenReturn(true);

        assertThrows(RegraNegocioException.class, () -> clienteService.cadastrarCliente(dto));
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void deveRejeitarCpfComDigitosInvalidosOuRepetidos() {
        for (String cpf : new String[] { "52998224726", "11111111111" }) {
            var dto = new ClienteRequestDTO("Maria", "22999999999", "maria@email.com", cpf);
            assertThrows(RegraNegocioException.class, () -> clienteService.cadastrarCliente(dto));
        }
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void deveRejeitarEmailMalformadoAntesDeSalvar() {
        var dto = new ClienteRequestDTO("Maria", "22999999999", "maria@-email.com", "52998224725");
        assertThrows(RegraNegocioException.class, () -> clienteService.cadastrarCliente(dto));
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void deveRejeitarTelefoneInvalidoAntesDeSalvar() {
        for (String telefone : new String[] { "", "abc", "999999999", "229999999999", "22abc99999999", "+1 22999999999" }) {
            var dto = new ClienteRequestDTO("Maria", telefone, "maria@email.com", "52998224725");
            assertThrows(RegraNegocioException.class, () -> clienteService.cadastrarCliente(dto));
        }
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void deveSalvarTelefoneComDddSemPontuacao() {
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        var dto = new ClienteRequestDTO("Maria", "+55 (22) 99999-9999", "maria@email.com", "52998224725");
        var resposta = clienteService.cadastrarCliente(dto);
        assertEquals("22999999999", resposta.telefone());
    }
}
