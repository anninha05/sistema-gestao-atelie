package com.atelie.sistema.usuario.dto;

import com.atelie.sistema.usuario.Perfil;

public record UsuarioRequestDTO(
        String nome,
        String email,
        String senha,
        Perfil perfil) {
}
