package com.atelie.sistema.ordemservico.dto;

import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.peca.TipoFluxo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrdemProducaoResponse(UUID id, String clienteNome, TipoFluxo tipoFluxo,
        StatusOrdemServico statusAtual, LocalDateTime dataEntrega, LocalDateTime inicioLocacao,
        LocalDateTime dataProva, LocalDateTime dataEvento, String medidas, String instrucoesCostura,
        List<Item> pecas) {
    public record Item(UUID pecaId, String medidas, String instrucoesCostura) {}
}
