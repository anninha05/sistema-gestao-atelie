package com.atelie.sistema.usuario;

import com.atelie.sistema.usuario.dto.UsuarioResponseDTO;
import java.security.Principal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
class AuthController {
    private final UsuarioService usuarios;

    AuthController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping("/csrf")
    CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @GetMapping("/me")
    UsuarioResponseDTO sessao(Principal principal) {
        return usuarios.buscarPorEmail(principal.getName());
    }
}
