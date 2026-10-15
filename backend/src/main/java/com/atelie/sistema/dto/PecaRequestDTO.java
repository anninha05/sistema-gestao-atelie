package com.atelie.sistema.dto;

import com.atelie.sistema.domain.model.TipoFluxo;

public record PecaRequestDTO(String descricao, TipoFluxo tipo, java.math.BigDecimal valorBase, String tamanho,
        String cor) {
}