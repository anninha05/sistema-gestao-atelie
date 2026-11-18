package com.atelie.sistema.precificacao.dto;

import com.atelie.sistema.precificacao.IndicadorPrecificacao;
import java.math.BigDecimal;
import java.time.YearMonth;

public record MetaMensalResponse(YearMonth mes, BigDecimal valorMeta, IndicadorPrecificacao indicador,
        Long responsavelId, String responsavelNome) {}
