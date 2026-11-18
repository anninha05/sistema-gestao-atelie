package com.atelie.sistema.precificacao.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SugestaoPrecoRequest(UUID ordemServicoId, BigDecimal pisoMinimo) {}
