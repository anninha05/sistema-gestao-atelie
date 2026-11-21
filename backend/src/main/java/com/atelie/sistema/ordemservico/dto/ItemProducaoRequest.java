package com.atelie.sistema.ordemservico.dto;

import java.util.UUID;

public record ItemProducaoRequest(UUID pecaId, String medidas, String instrucoesCostura) {}
