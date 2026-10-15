package com.atelie.sistema.service;

import com.atelie.sistema.domain.model.Peca;
import com.atelie.sistema.domain.repository.PecaRepositoryGateway;
import com.atelie.sistema.dto.PecaRequestDTO;
import com.atelie.sistema.dto.PecaResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PecaService {

    private final PecaRepositoryGateway pecaGateway;

    public PecaService(PecaRepositoryGateway pecaGateway) {
        this.pecaGateway = pecaGateway;
    }

    @Transactional
    public PecaResponseDTO cadastrarPeca(PecaRequestDTO dto) {
        Peca novaPeca = new Peca(dto.descricao(), dto.tipo());
        if (dto.valorBase() != null) {
            novaPeca.setValorBase(dto.valorBase());
        }
        if (dto.tamanho() != null) {
            novaPeca.setTamanho(dto.tamanho());
        }
        if (dto.cor() != null) {
            novaPeca.setCor(dto.cor());
        }
        Peca pecaSalva = pecaGateway.salvar(novaPeca);

        return new PecaResponseDTO(pecaSalva.getId(), pecaSalva.getDescricao(), pecaSalva.getTipo(),
                pecaSalva.getStatusDisponibilidade());
    }

    public List<PecaResponseDTO> listarDisponiveis() {
        return pecaGateway.buscarDisponiveis().stream()
                .map(p -> new PecaResponseDTO(p.getId(), p.getDescricao(), p.getTipo(), p.getStatusDisponibilidade()))
                .toList();
    }
}