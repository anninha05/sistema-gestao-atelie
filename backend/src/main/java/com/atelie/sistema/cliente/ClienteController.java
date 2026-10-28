package com.atelie.sistema.cliente;

import com.atelie.sistema.cliente.dto.ClienteRequestDTO;
import com.atelie.sistema.cliente.dto.ClienteResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> cadastrar(@RequestBody ClienteRequestDTO dto) {

        ClienteResponseDTO resposta = clienteService.cadastrarCliente(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }
}