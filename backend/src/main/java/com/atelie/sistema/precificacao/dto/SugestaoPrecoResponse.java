package com.atelie.sistema.precificacao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SugestaoPrecoResponse(Long id, UUID ordemServicoId, UUID clienteId, Long execucaoPidId,
        BigDecimal precoBase, double ajustePid, BigDecimal precoAjustado, BigDecimal descontoFidelidade,
        BigDecimal precoAposFidelidade, BigDecimal pisoMinimo, BigDecimal precoSugerido,
        int ordensConcluidas, int ordensCanceladas, boolean elegivelFidelidade, String justificativa,
        Long responsavelId, String responsavelNome, LocalDateTime criadaEm) {}
