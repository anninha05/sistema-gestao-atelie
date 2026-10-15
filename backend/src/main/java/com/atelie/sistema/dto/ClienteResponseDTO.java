package com.atelie.sistema.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClienteResponseDTO(
        UUID id,
        String nome,
        String telefone,
        String email,
        String cpf,
        LocalDateTime criadoEm) {
}