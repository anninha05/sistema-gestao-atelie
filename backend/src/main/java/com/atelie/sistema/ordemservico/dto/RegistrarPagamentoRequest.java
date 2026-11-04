package com.atelie.sistema.ordemservico.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarPagamentoRequest(
        BigDecimal valor,
        LocalDate data,
        com.atelie.sistema.ordemservico.MetodoPagamento metodo) {
}

