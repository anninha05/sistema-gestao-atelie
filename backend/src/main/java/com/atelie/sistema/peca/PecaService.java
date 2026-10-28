package com.atelie.sistema.peca;

import com.atelie.sistema.peca.dto.PecaRequestDTO;
import com.atelie.sistema.peca.dto.PecaResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PecaService {

    private final PecaRepository pecaRepository;

    public PecaService(PecaRepository pecaRepository) {
        this.pecaRepository = pecaRepository;
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
        Peca pecaSalva = pecaRepository.save(novaPeca);

        return new PecaResponseDTO(pecaSalva.getId(), pecaSalva.getDescricao(), pecaSalva.getTipo(),
                pecaSalva.getStatusDisponibilidade());
    }

    public List<PecaResponseDTO> listarDisponiveis() {
        return pecaRepository.findByStatusDisponibilidade(StatusDisponibilidade.DISPONIVEL).stream()
                .map(p -> new PecaResponseDTO(p.getId(), p.getDescricao(), p.getTipo(), p.getStatusDisponibilidade()))
                .toList();
    }
}