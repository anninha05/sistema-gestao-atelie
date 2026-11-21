package com.atelie.sistema.financeiro;

import java.time.YearMonth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/financeiro/indicadores")
class IndicadoresFinanceirosController {
    private final IndicadoresFinanceirosService indicadores;

    IndicadoresFinanceirosController(IndicadoresFinanceirosService indicadores) {
        this.indicadores = indicadores;
    }

    @GetMapping
    IndicadoresFinanceirosResponse consultar(@RequestParam(required = false) YearMonth mes) {
        return indicadores.consultar(mes);
    }
}
