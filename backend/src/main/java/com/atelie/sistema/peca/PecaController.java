package com.atelie.sistema.peca;

import com.atelie.sistema.peca.dto.PecaRequestDTO;
import com.atelie.sistema.peca.dto.PecaResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @GetMapping
    public ResponseEntity<List<PecaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(pecaService.listarTodas());
    }

    @GetMapping("/disponiveis")
    public ResponseEntity<List<PecaResponseDTO>> listarDisponiveis() {
        return ResponseEntity.ok(pecaService.listarDisponiveis());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PecaResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(pecaService.buscarPorId(id));
    }

    @PatchMapping("/{id}/alugar")
    public ResponseEntity<PecaResponseDTO> alugar(@PathVariable UUID id) {
        return ResponseEntity.ok(pecaService.marcarComoAlugada(id));
    }

    @PatchMapping("/{id}/costura")
    public ResponseEntity<PecaResponseDTO> costura(@PathVariable UUID id) {
        return ResponseEntity.ok(pecaService.enviarParaCostura(id));
    }

    @PatchMapping("/{id}/lavagem")
    public ResponseEntity<PecaResponseDTO> lavagem(@PathVariable UUID id) {
        return ResponseEntity.ok(pecaService.enviarParaLavagem(id));
    }

    @PatchMapping("/{id}/devolver")
    public ResponseEntity<PecaResponseDTO> devolver(@PathVariable UUID id) {
        return ResponseEntity.ok(pecaService.devolverParaEstoque(id));
    }
}