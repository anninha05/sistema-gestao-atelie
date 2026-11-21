package com.atelie.sistema.ordemservico.dto;

import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.peca.TipoFluxo;
import java.time.LocalDateTime;
import java.util.UUID;

public record CompromissoResponse(UUID ordemId, String clienteNome, TipoFluxo tipoFluxo,
        String tipoCompromisso, LocalDateTime dataHora, StatusOrdemServico statusAtual) {}
