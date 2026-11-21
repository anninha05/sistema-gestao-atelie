package com.atelie.sistema.ordemservico;

import com.atelie.sistema.ordemservico.dto.OrdemProducaoResponse;
import com.atelie.sistema.ordemservico.dto.OrdemServicoRequestDTO;
import com.atelie.sistema.ordemservico.dto.OrdemServicoResponseDTO;
import com.atelie.sistema.ordemservico.dto.PagamentoResponse;
import com.atelie.sistema.ordemservico.dto.ReagendarOrdemServicoRequest;
import com.atelie.sistema.ordemservico.dto.RegistrarPagamentoRequest;
import com.atelie.sistema.usuario.Perfil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ordens-servico")
public class OrdemServicoController {

    private final OrdemServicoService ordemServicoService;

    public OrdemServicoController(OrdemServicoService ordemServicoService) {
        this.ordemServicoService = ordemServicoService;
    }

    @PostMapping
    public ResponseEntity<OrdemServicoResponseDTO> abrir(@RequestBody OrdemServicoRequestDTO dto, Authentication usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ordemServicoService.abrirOrdemServico(dto).paraPerfil(Perfil.de(usuario)));
    }

    @GetMapping
    public List<OrdemServicoResponseDTO> listar(Authentication usuario) {
        Perfil perfil = Perfil.de(usuario);
        return ordemServicoService.listarTodas().stream().map(ordem -> ordem.paraPerfil(perfil)).toList();
    }

    @GetMapping("/producao")
    public List<OrdemProducaoResponse> listarFilaProducao(Authentication usuario) {
        Perfil perfil = Perfil.de(usuario);
        return ordemServicoService.listarFilaProducao().stream().map(ordem -> ordem.paraPerfil(perfil)).toList();
    }

    @GetMapping("/disponibilidade")
    public Map<String, Boolean> consultarDisponibilidade(@RequestParam UUID pecaId,
            @RequestParam LocalDateTime inicio, @RequestParam LocalDateTime fim) {
        return Map.of("disponivel", ordemServicoService.disponivelNoPeriodo(pecaId, inicio, fim));
    }

    @GetMapping("/{id}")
    public OrdemServicoResponseDTO buscarPorId(@PathVariable UUID id, Authentication usuario) {
        return ordemServicoService.buscarPorId(id).paraPerfil(Perfil.de(usuario));
    }

    @PatchMapping("/{id}/reagendar")
    public OrdemServicoResponseDTO reagendar(@PathVariable UUID id,
            @RequestBody ReagendarOrdemServicoRequest request, Authentication usuario) {
        return ordemServicoService.reagendar(id, request).paraPerfil(Perfil.de(usuario));
    }

    @PatchMapping("/{id}/confeccao")
    public Object iniciarConfeccao(@PathVariable UUID id, Authentication usuario) {
        ordemServicoService.iniciarConfeccao(id);
        return respostaAtualizada(id, usuario);
    }

    @PatchMapping("/{id}/prova")
    public Object enviarParaProva(@PathVariable UUID id, Authentication usuario) {
        ordemServicoService.enviarParaProva(id);
        return respostaAtualizada(id, usuario);
    }

    @PatchMapping("/{id}/passadoria")
    public Object iniciarPassadoria(@PathVariable UUID id, Authentication usuario) {
        ordemServicoService.iniciarPassadoria(id);
        return respostaAtualizada(id, usuario);
    }

    @PatchMapping("/{id}/concluir")
    public Object concluir(@PathVariable UUID id, Authentication usuario) {
        ordemServicoService.concluir(id);
        return respostaAtualizada(id, usuario);
    }

    @PatchMapping("/{id}/cancelar")
    public OrdemServicoResponseDTO cancelar(@PathVariable UUID id, Authentication usuario) {
        return ordemServicoService.cancelar(id).paraPerfil(Perfil.de(usuario));
    }

    @PatchMapping("/{id}/retirar")
    public OrdemServicoResponseDTO retirar(@PathVariable UUID id, Authentication usuario) {
        return ordemServicoService.retirar(id).paraPerfil(Perfil.de(usuario));
    }

    @PatchMapping("/{id}/devolver")
    public OrdemServicoResponseDTO devolver(@PathVariable UUID id, Authentication usuario) {
        return ordemServicoService.devolver(id).paraPerfil(Perfil.de(usuario));
    }

    @PostMapping("/{id}/pagamentos/sinal")
    public PagamentoResponse registrarSinal(@PathVariable UUID id, @RequestBody RegistrarPagamentoRequest request) {
        return ordemServicoService.registrarSinal(id, request);
    }

    @PostMapping("/{id}/pagamentos")
    public PagamentoResponse registrarPagamento(@PathVariable UUID id, @RequestBody RegistrarPagamentoRequest request) {
        return ordemServicoService.registrarPagamento(id, request);
    }

    @GetMapping("/{id}/pagamentos")
    public PagamentoResponse buscarPagamento(@PathVariable UUID id) {
        return ordemServicoService.buscarPagamento(id);
    }

    private Object respostaAtualizada(UUID id, Authentication usuario) {
        Perfil perfil = Perfil.de(usuario);
        return perfil == Perfil.ROLE_PRODUCAO
                ? ordemServicoService.buscarParaProducao(id).paraPerfil(perfil)
                : ordemServicoService.buscarPorId(id).paraPerfil(perfil);
    }
}
