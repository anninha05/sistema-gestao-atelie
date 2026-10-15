package com.atelie.sistema.dto;

public record ClienteRequestDTO(
                String nome,
                String telefone,
                String email,
                String cpf) {
}