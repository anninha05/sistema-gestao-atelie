package com.atelie.sistema.cliente;

import com.atelie.sistema.cliente.dto.ClienteRequestDTO;
import com.atelie.sistema.cliente.dto.ClienteResponseDTO;
import com.atelie.sistema.ordemservico.OrdemServicoService;
import com.atelie.sistema.ordemservico.dto.OrdemServicoResponseDTO;
import com.atelie.sistema.usuario.Perfil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final OrdemServicoService ordemServicoService;

    public ClienteController(ClienteService clienteService, OrdemServicoService ordemServicoService) {
        this.clienteService = clienteService;
        this.ordemServicoService = ordemServicoService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> cadastrar(@RequestBody ClienteRequestDTO dto) {
        ClienteResponseDTO resposta = clienteService.cadastrarCliente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> listar() {
        return ResponseEntity.ok(clienteService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> atualizar(@PathVariable UUID id, @RequestBody ClienteRequestDTO dto) {
        return ResponseEntity.ok(clienteService.atualizar(id, dto));
    }

    @GetMapping("/{id}/ordens-servico")
    public List<OrdemServicoResponseDTO> historico(@PathVariable UUID id, Authentication usuario) {
        Perfil perfil = Perfil.de(usuario);
        return ordemServicoService.listarPorCliente(id).stream().map(ordem -> ordem.paraPerfil(perfil)).toList();
    }
}
