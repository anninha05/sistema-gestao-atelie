package com.atelie.sistema.domain.repository;

import com.atelie.sistema.domain.model.Usuario;
import java.util.Optional;

public interface UsuarioRepositoryGateway {
    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorEmail(String email);

    boolean existePorEmail(String email);
}