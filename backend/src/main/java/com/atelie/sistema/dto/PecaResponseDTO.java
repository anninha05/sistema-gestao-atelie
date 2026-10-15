package com.atelie.sistema.dto;

import com.atelie.sistema.domain.model.StatusDisponibilidade;
import com.atelie.sistema.domain.model.TipoFluxo;
import java.util.UUID;

public record PecaResponseDTO(UUID id, String descricao, TipoFluxo tipo, StatusDisponibilidade status) {
}