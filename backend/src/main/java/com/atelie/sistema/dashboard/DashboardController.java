package com.atelie.sistema.dashboard;

import com.atelie.sistema.ordemservico.OrdemServicoService;
import com.atelie.sistema.ordemservico.dto.CompromissoResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    private final OrdemServicoService ordens;

    public DashboardController(OrdemServicoService ordens) {
        this.ordens = ordens;
    }

    @GetMapping("/compromissos")
    public ResponseEntity<List<CompromissoResponse>> compromissos(@RequestParam LocalDate data) {
        return ResponseEntity.ok(ordens.compromissosDoDia(data));
    }
}
