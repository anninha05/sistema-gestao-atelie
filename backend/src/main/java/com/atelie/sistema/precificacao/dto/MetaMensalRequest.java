package com.atelie.sistema.precificacao.dto;

import com.atelie.sistema.precificacao.IndicadorPrecificacao;
import java.math.BigDecimal;

public record MetaMensalRequest(BigDecimal valorMeta, IndicadorPrecificacao indicador) {}
