package com.atelie.sistema.usuario.dto;

import com.atelie.sistema.usuario.Perfil;
import java.time.LocalDateTime;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        Perfil perfil,
        LocalDateTime criadoEm) {
}
