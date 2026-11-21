package com.atelie.sistema.ordemservico.dto;

import java.time.LocalDateTime;

public record ReagendarOrdemServicoRequest(
        LocalDateTime inicioLocacao,
        LocalDateTime fimLocacao,
        LocalDateTime dataProva,
        LocalDateTime dataEvento,
        LocalDateTime dataEntrega) {
}
