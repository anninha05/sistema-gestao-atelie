package com.atelie.sistema.usuario;

import java.util.Optional;

public interface UsuarioRepositoryGateway {
    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorEmail(String email);

    boolean existePorEmail(String email);
}