package com.atelie.sistema.usuario;

import com.atelie.sistema.shared.exception.RegraNegocioException;
import com.atelie.sistema.usuario.dto.UsuarioRequestDTO;
import com.atelie.sistema.usuario.dto.UsuarioResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4));
    }

    @Test
    @DisplayName("Deve cadastrar usuário com sucesso quando e-mail não existir")
    void deveCadastrarUsuarioComSucesso() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Admin", "admin@atelie.com", "senha-teste-123", Perfil.ROLE_ADMIN);
        Usuario usuarioSalvo = new Usuario("Admin", "admin@atelie.com", "senha-teste-123", Perfil.ROLE_ADMIN);

        when(usuarioRepository.existsByEmail("admin@atelie.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioSalvo);

        UsuarioResponseDTO response = usuarioService.cadastrarUsuario(dto);

        assertNotNull(response);
        assertEquals("admin@atelie.com", response.email());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar RegraNegocioException ao cadastrar com e-mail já existente")
    void deveLancarExcecaoQuandoEmailDuplicado() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Admin", "admin@atelie.com", "senha-teste-123", Perfil.ROLE_ADMIN);

        when(usuarioRepository.existsByEmail("admin@atelie.com")).thenReturn(true);

        assertThrows(RegraNegocioException.class, () -> usuarioService.cadastrarUsuario(dto));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
