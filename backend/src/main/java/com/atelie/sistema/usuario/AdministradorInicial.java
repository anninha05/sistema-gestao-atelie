package com.atelie.sistema.usuario;

import com.atelie.sistema.usuario.dto.UsuarioRequestDTO;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
class AdministradorInicial implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final UsuarioService cadastro;
    private final String email;
    private final String senha;

    AdministradorInicial(UsuarioRepository usuarios, UsuarioService cadastro,
            @Value("${ADMIN_EMAIL:}") String email, @Value("${ADMIN_PASSWORD:}") String senha) {
        this.usuarios = usuarios;
        this.cadastro = cadastro;
        this.email = email.strip().toLowerCase(Locale.ROOT);
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isEmpty() && senha.isEmpty()) {
            return;
        }
        if (email.isEmpty() || senha.isEmpty()) {
            throw new IllegalStateException("Configure ADMIN_EMAIL e ADMIN_PASSWORD juntos.");
        }
        if (!usuarios.existsByEmail(email)) {
            cadastro.cadastrarUsuario(new UsuarioRequestDTO("Administrador", email, senha, Perfil.ROLE_ADMIN));
        }
    }
}
