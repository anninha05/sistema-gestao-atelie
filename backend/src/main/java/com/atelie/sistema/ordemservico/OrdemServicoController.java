package com.atelie.sistema.ordemservico;

import com.atelie.sistema.ordemservico.dto.OrdemServicoRequestDTO;
import com.atelie.sistema.ordemservico.dto.OrdemServicoResponseDTO;
import com.atelie.sistema.ordemservico.dto.PagamentoResponse;
import com.atelie.sistema.ordemservico.dto.RegistrarPagamentoRequest;
import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ordens-servico")
public class OrdemServicoController {

    private final OrdemServicoService ordemServicoService;

    public OrdemServicoController(OrdemServicoService ordemServicoService) {
        this.ordemServicoService = ordemServicoService;
    }

    @PostMapping
    public ResponseEntity<OrdemServicoResponseDTO> abrir(@RequestBody OrdemServicoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordemServicoService.abrirOrdemServico(dto));
    }

    @GetMapping
    public ResponseEntity<List<OrdemServicoResponseDTO>> listar() {
        return ResponseEntity.ok(ordemServicoService.listarTodas());
    }

    @GetMapping("/disponibilidade")
    public java.util.Map<String, Boolean> consultarDisponibilidade(
            @RequestParam UUID pecaId,
            @RequestParam java.time.LocalDateTime inicio,
            @RequestParam java.time.LocalDateTime fim) {
        return java.util.Map.of("disponivel", ordemServicoService.disponivelNoPeriodo(pecaId, inicio, fim));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdemServicoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ordemServicoService.buscarPorId(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrdemServicoResponseDTO>> listarPorStatus(@PathVariable StatusOrdemServico status) {
        return ResponseEntity.ok(ordemServicoService.listarPorStatus(status));
    }

    @PatchMapping("/{id}/confeccao")
    public ResponseEntity<OrdemServicoResponseDTO> iniciarConfeccao(@PathVariable UUID id) {
        return ResponseEntity.ok(ordemServicoService.iniciarConfeccao(id));
    }

    @PatchMapping("/{id}/prova")
    public ResponseEntity<OrdemServicoResponseDTO> enviarParaProva(@PathVariable UUID id) {
        return ResponseEntity.ok(ordemServicoService.enviarParaProva(id));
    }

    @PatchMapping("/{id}/passadoria")
    public OrdemServicoResponseDTO iniciarPassadoria(@PathVariable UUID id) {
        return ordemServicoService.iniciarPassadoria(id);
    }

    @PatchMapping("/{id}/cancelar")
    public OrdemServicoResponseDTO cancelar(@PathVariable UUID id) {
        return ordemServicoService.cancelar(id);
    }

    @PatchMapping("/{id}/concluir")
    public ResponseEntity<OrdemServicoResponseDTO> concluir(@PathVariable UUID id) {
        return ResponseEntity.ok(ordemServicoService.concluir(id));
    }

    @PatchMapping("/{id}/retirar")
    public ResponseEntity<OrdemServicoResponseDTO> retirar(@PathVariable UUID id) {
        return ResponseEntity.ok(ordemServicoService.retirar(id));
    }

    @PatchMapping("/{id}/devolver")
    public ResponseEntity<OrdemServicoResponseDTO> devolver(@PathVariable UUID id) {
        return ResponseEntity.ok(ordemServicoService.devolver(id));
    }

    @PostMapping("/{id}/pagamentos/sinal")
    public ResponseEntity<PagamentoResponse> registrarSinal(
            @PathVariable UUID id,
            @RequestBody RegistrarPagamentoRequest request) {
        return ResponseEntity.ok(ordemServicoService.registrarSinal(id, request));
    }

    @PostMapping("/{id}/pagamentos")
    public ResponseEntity<PagamentoResponse> registrarPagamento(
            @PathVariable UUID id,
            @RequestBody RegistrarPagamentoRequest request) {
        return ResponseEntity.ok(ordemServicoService.registrarPagamento(id, request));
    }

    @GetMapping("/{id}/pagamentos")
    public ResponseEntity<PagamentoResponse> buscarPagamento(@PathVariable UUID id) {
        return ResponseEntity.ok(ordemServicoService.buscarPagamento(id));
    }
}
