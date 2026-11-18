package com.atelie.sistema.peca;

import com.atelie.sistema.peca.dto.PecaRequestDTO;
import com.atelie.sistema.peca.dto.LiberacaoPecaRequestDTO;
import com.atelie.sistema.peca.dto.PecaResponseDTO;
import com.atelie.sistema.peca.dto.PecaProducaoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

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

    @GetMapping("/producao")
    public ResponseEntity<List<PecaProducaoResponse>> listarParaProducao() {
        return ResponseEntity.ok(pecaService.listarParaProducao());
    }

    @GetMapping("/disponiveis")
    public ResponseEntity<List<PecaResponseDTO>> listarDisponiveis() {
        return ResponseEntity.ok(pecaService.listarDisponiveis());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PecaResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(pecaService.buscarPorId(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PecaResponseDTO> atualizar(@PathVariable UUID id, @RequestBody PecaRequestDTO dto) {
        return ResponseEntity.ok(pecaService.atualizar(id, dto));
    }

    @PatchMapping("/{id}/costura")
    public ResponseEntity<?> costura(@PathVariable UUID id, Authentication usuario) {
        pecaService.enviarParaCostura(id);
        return ResponseEntity.ok(respostaAtualizada(id, usuario));
    }

    @PatchMapping("/{id}/lavagem")
    public ResponseEntity<?> lavagem(@PathVariable UUID id, Authentication usuario) {
        pecaService.enviarParaLavagem(id);
        return ResponseEntity.ok(respostaAtualizada(id, usuario));
    }

    @PatchMapping("/{id}/devolver")
    public ResponseEntity<?> devolver(@PathVariable UUID id,
            @RequestBody(required = false) LiberacaoPecaRequestDTO confirmacao, Authentication usuario) {
        pecaService.devolverParaEstoque(id, confirmacao);
        return ResponseEntity.ok(respostaAtualizada(id, usuario));
    }

    private Object respostaAtualizada(UUID id, Authentication usuario) {
        boolean producao = usuario.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PRODUCAO"));
        return producao ? pecaService.buscarParaProducao(id) : pecaService.buscarPorId(id);
    }
}
