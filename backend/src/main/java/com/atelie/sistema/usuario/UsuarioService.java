package com.atelie.sistema.usuario;

import com.atelie.sistema.shared.exception.RegraNegocioException;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.usuario.dto.UsuarioRequestDTO;
import com.atelie.sistema.usuario.dto.UsuarioResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder senhas;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder senhas) {
        this.usuarioRepository = usuarioRepository;
        this.senhas = senhas;
    }

    @Transactional
    public UsuarioResponseDTO cadastrarUsuario(UsuarioRequestDTO dto) {
        if (dto.nome() == null || dto.nome().isBlank() || dto.nome().length() > 255
                || dto.email() == null || !dto.email().strip().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || dto.email().length() > 255 || dto.perfil() == null || dto.senha() == null
                || dto.senha().length() < 8 || dto.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegraNegocioException("Informe nome, e-mail, perfil e uma senha válida com ao menos oito caracteres.");
        }
        String email = dto.email().strip().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe um usuário cadastrado com este e-mail.");
        }

        Usuario novoUsuario = new Usuario(
                dto.nome(),
                email,
                senhas.encode(dto.senha()),
                dto.perfil());

        Usuario usuarioSalvo = usuarioRepository.save(novoUsuario);

        return toResponseDTO(usuarioSalvo);
    }

    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com id: " + id));
        return toResponseDTO(usuario);
    }

    public UsuarioResponseDTO buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email).map(this::toResponseDTO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getCriadoEm());
    }
}
