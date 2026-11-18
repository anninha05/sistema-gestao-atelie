package com.atelie.sistema.shared.config;

import com.atelie.sistema.shared.exception.GlobalExceptionHandler.ErroResponse;
import com.atelie.sistema.usuario.UsuarioRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import tools.jackson.databind.ObjectMapper;

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
        SecurityFilterChain api(HttpSecurity http, ObjectMapper json) throws Exception {
                http.cors(cors -> {
                })
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/auth/csrf", "/auth/login").permitAll()
                                                .requestMatchers("/usuarios/**", "/financeiro/**", "/precificacao/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/ordens-servico/*/aprovar-preco").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/dashboard/**")
                                                .hasAnyRole("ADMIN", "ATENDENTE")
                                                .requestMatchers(HttpMethod.GET, "/ordens-servico/producao",
                                                                "/pecas/producao")
                                                .hasAnyRole("ADMIN", "PRODUCAO")
                                                .requestMatchers("/ordens-servico/*/pagamentos/**")
                                                .hasAnyRole("ADMIN", "ATENDENTE")
                                                .requestMatchers(HttpMethod.PATCH, "/pecas/*/devolver")
                                                .hasAnyRole("ADMIN", "ATENDENTE", "PRODUCAO")
                                                .requestMatchers(HttpMethod.PATCH, "/clientes/*", "/pecas/*")
                                                .hasAnyRole("ADMIN", "ATENDENTE")
                                                .requestMatchers(HttpMethod.PATCH, "/ordens-servico/*/confeccao",
                                                                "/ordens-servico/*/prova",
                                                                "/ordens-servico/*/passadoria",
                                                                "/ordens-servico/*/concluir", "/pecas/*/costura",
                                                                "/pecas/*/lavagem")
                                                .hasAnyRole("ADMIN", "PRODUCAO")
                                                .requestMatchers(HttpMethod.GET, "/ordens-servico/**", "/pecas/**")
                                                .hasAnyRole("ADMIN", "ATENDENTE")
                                                .requestMatchers("/ordens-servico/**", "/pecas/**", "/clientes/**")
                                                .hasAnyRole("ADMIN", "ATENDENTE")
                                                .requestMatchers("/auth/me").authenticated()
                                                .anyRequest().denyAll())
                                .exceptionHandling(errors -> errors
                                                .authenticationEntryPoint((request, response,
                                                                exception) -> responderErro(response, json,
                                                                                HttpStatus.UNAUTHORIZED,
                                                                                "Sua sessão expirou ou não foi iniciada. Entre novamente."))
                                                .accessDeniedHandler((request, response, exception) -> responderErro(
                                                                response, json,
                                                                HttpStatus.FORBIDDEN, exception instanceof CsrfException
                                                                                ? "A sessão de segurança expirou. Atualize a página e tente novamente."
                                                                                : "Você não tem permissão para realizar esta operação.")))
                                .formLogin(login -> login.loginProcessingUrl("/auth/login")
                                                .usernameParameter("email")
                                                .successHandler((request, response, authentication) -> response
                                                                .setStatus(204))
                                                .failureHandler((request, response, exception) -> response
                                                                .setStatus(401)))
                                .logout(logout -> logout.logoutUrl("/auth/logout")
                                                .logoutSuccessHandler((request, response, authentication) -> response
                                                                .setStatus(204)));
                return http.build();
        }

        private void responderErro(HttpServletResponse response, ObjectMapper json, HttpStatus status, String mensagem)
                        throws IOException {
                response.setStatus(status.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                json.writeValue(response.getOutputStream(), ErroResponse.de(status, mensagem));
        }
}
