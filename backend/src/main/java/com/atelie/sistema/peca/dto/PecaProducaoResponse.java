package com.atelie.sistema.peca.dto;

import com.atelie.sistema.peca.state.StatusDisponibilidade;
import java.util.UUID;

public record PecaProducaoResponse(UUID id, String descricao, String codigoEtiqueta,
        String tamanho, String cor, StatusDisponibilidade status) {}
