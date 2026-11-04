package com.atelie.sistema.peca.dto;

import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.peca.state.StatusDisponibilidade;
import java.util.UUID;

public record PecaResponseDTO(UUID id, String descricao, TipoFluxo tipo, StatusDisponibilidade status, String codigoEtiqueta, String tamanho, String cor, java.math.BigDecimal valorBase) {
}