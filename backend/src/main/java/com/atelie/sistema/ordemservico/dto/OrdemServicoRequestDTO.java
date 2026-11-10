package com.atelie.sistema.ordemservico.dto;

import com.atelie.sistema.peca.TipoFluxo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrdemServicoRequestDTO(
        UUID clienteId,
        TipoFluxo tipoFluxo,
        List<UUID> pecas,
        BigDecimal desconto,
        LocalDateTime inicioLocacao,
        LocalDateTime fimLocacao,
        LocalDateTime dataProva,
        LocalDateTime dataEvento,
        String instrucoesCostura,
        String medidas,
        LocalDateTime dataEntrega,
        BigDecimal descontoPercentual) {
}
