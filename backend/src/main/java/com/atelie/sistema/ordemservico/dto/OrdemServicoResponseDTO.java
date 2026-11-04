package com.atelie.sistema.ordemservico.dto;

import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.peca.TipoFluxo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.atelie.sistema.ordemservico.ItemOrdemServico;

public record OrdemServicoResponseDTO(
        UUID id,
        UUID clienteId,
        String clienteNome,
        TipoFluxo tipoFluxo,
        StatusOrdemServico statusAtual,
        BigDecimal valorTotal,
        LocalDateTime dataEntrega,
        LocalDateTime criadoEm,
        List<ItemOrdemServico> itens,
        BigDecimal desconto,
        LocalDateTime inicioLocacao,
        LocalDateTime fimLocacao,
        LocalDateTime dataProva,
        LocalDateTime dataEvento,
        String instrucoesCostura,
        String medidas) {
}
