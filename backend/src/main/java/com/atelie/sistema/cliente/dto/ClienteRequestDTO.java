package com.atelie.sistema.cliente.dto;

public record ClienteRequestDTO(
                String nome,
                String telefone,
                String email,
                String cpf) {
}