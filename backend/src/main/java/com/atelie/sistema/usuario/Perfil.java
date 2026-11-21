package com.atelie.sistema.usuario;

import java.util.Arrays;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public enum Perfil {
    ROLE_ADMIN,
    ROLE_ATENDENTE,
    ROLE_PRODUCAO;

    public static Perfil de(Authentication autenticacao) {
        return autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .flatMap(nome -> Arrays.stream(values()).filter(perfil -> perfil.name().equals(nome)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado sem perfil reconhecido."));
    }
}
