package com.atelie.sistema.precificacao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AprovacaoPrecoResponse(Long id, UUID ordemServicoId, Long sugestaoId,
        BigDecimal valorAnterior, BigDecimal valorAprovado, BigDecimal valorRecebido,
        Long responsavelId, String responsavelNome, LocalDateTime aprovadaEm) {}
