package com.atelie.sistema.peca.dto;

import com.atelie.sistema.peca.StatusDisponibilidade;
import com.atelie.sistema.peca.TipoFluxo;
import java.util.UUID;

public record PecaResponseDTO(UUID id, String descricao, TipoFluxo tipo, StatusDisponibilidade status) {
}