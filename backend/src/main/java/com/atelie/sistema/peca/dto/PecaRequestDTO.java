package com.atelie.sistema.peca.dto;

import com.atelie.sistema.peca.TipoFluxo;

public record PecaRequestDTO(String descricao, TipoFluxo tipo, java.math.BigDecimal valorBase, String tamanho,
        String cor) {
}