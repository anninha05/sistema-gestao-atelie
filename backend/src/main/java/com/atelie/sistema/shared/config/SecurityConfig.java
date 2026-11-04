package com.atelie.sistema.shared.config;

import com.atelie.sistema.usuario.UsuarioRepository;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService usuariosParaLogin(UsuarioRepository usuarios) {
        return email -> usuarios.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .map(usuario -> User.withUsername(usuario.getEmail())
                        .password(usuario.getSenha())
                        .authorities(usuario.getPerfil().name()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
    }

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http.cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/csrf", "/auth/login").permitAll()
                        .requestMatchers("/usuarios/**", "/financeiro/**").hasRole("ADMIN")
                        .requestMatchers("/ordens-servico/*/pagamentos/**").hasAnyRole("ADMIN", "ATENDENTE")
                        .requestMatchers(HttpMethod.PATCH, "/ordens-servico/*/confeccao",
                                "/ordens-servico/*/prova", "/ordens-servico/*/passadoria",
                                "/ordens-servico/*/concluir", "/pecas/*/costura",
                                "/pecas/*/lavagem").hasAnyRole("ADMIN", "PRODUCAO")
                        .requestMatchers(HttpMethod.GET, "/ordens-servico/**", "/pecas/**").authenticated()
                        .requestMatchers("/ordens-servico/**", "/pecas/**", "/clientes/**")
                                .hasAnyRole("ADMIN", "ATENDENTE")
                        .requestMatchers("/auth/me").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) ->
                        response.setStatus(401)))
                .formLogin(login -> login.loginProcessingUrl("/auth/login")
                        .usernameParameter("email")
                        .successHandler((request, response, authentication) -> response.setStatus(204))
                        .failureHandler((request, response, exception) -> response.setStatus(401)))
                .logout(logout -> logout.logoutUrl("/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));
        return http.build();
    }
}
