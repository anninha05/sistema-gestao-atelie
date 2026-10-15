package com.atelie.sistema.controller;

import com.atelie.sistema.dto.PecaRequestDTO;
import com.atelie.sistema.dto.PecaResponseDTO;
import com.atelie.sistema.service.PecaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pecas")
public class PecaController {

    private final PecaService pecaService;

    public PecaController(PecaService pecaService) {
        this.pecaService = pecaService;
    }

    @PostMapping
    public ResponseEntity<PecaResponseDTO> cadastrar(@RequestBody PecaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pecaService.cadastrarPeca(dto));
    }

    @GetMapping("/disponiveis")
    public ResponseEntity<List<PecaResponseDTO>> listarDisponiveis() {
        return ResponseEntity.ok(pecaService.listarDisponiveis());
    }
}