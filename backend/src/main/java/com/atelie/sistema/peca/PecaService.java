package com.atelie.sistema.peca;

import com.atelie.sistema.peca.dto.PecaRequestDTO;
import com.atelie.sistema.peca.dto.LiberacaoPecaRequestDTO;
import com.atelie.sistema.peca.dto.PecaResponseDTO;
import com.atelie.sistema.peca.state.StatusDisponibilidade;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PecaService {

    private final PecaRepository pecaRepository;

    public PecaService(PecaRepository pecaRepository) {
        this.pecaRepository = pecaRepository;
    }

    @Transactional
    public PecaResponseDTO cadastrarPeca(PecaRequestDTO dto) {
        if (dto.tipo() == null || dto.descricao() == null || dto.descricao().isBlank()
                || dto.descricao().length() > 255
                || (dto.tamanho() != null && dto.tamanho().length() > 10)
                || (dto.cor() != null && dto.cor().length() > 50)
                || (dto.valorBase() != null && (dto.valorBase().signum() < 0
                    || dto.valorBase().stripTrailingZeros().scale() > 2
                    || dto.valorBase().compareTo(new java.math.BigDecimal("99999999.99")) > 0))) {
            throw new com.atelie.sistema.shared.exception.RegraNegocioException("Dados da peça inválidos.");
        }
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

        return toResponseDTO(pecaSalva);
    }

    public List<PecaResponseDTO> listarDisponiveis() {
        return pecaRepository.findByStatusDisponibilidade(StatusDisponibilidade.DISPONIVEL).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<PecaResponseDTO> listarTodas() {
        return pecaRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public PecaResponseDTO buscarPorId(UUID id) {
        return toResponseDTO(buscarEntidadePorId(id));
    }

    @Transactional
    public PecaResponseDTO atualizar(UUID id, PecaRequestDTO dto) {
        validarDados(dto, false);
        Peca peca = buscarParaAtualizacao(id);
        peca.atualizarDadosDoCatalogo(dto.descricao().strip(), dto.valorBase(),
                normalizarOpcional(dto.tamanho()), normalizarOpcional(dto.cor()));
        return toResponseDTO(peca);
    }

    private void validarDados(PecaRequestDTO dto, boolean exigirTipo) {
        if (dto == null || (exigirTipo && dto.tipo() == null) || dto.descricao() == null || dto.descricao().isBlank()
                || dto.descricao().length() > 255
                || (dto.tamanho() != null && dto.tamanho().length() > 10)
                || (dto.cor() != null && dto.cor().length() > 50)
                || dto.valorBase() == null || dto.valorBase().signum() < 0
                || dto.valorBase().stripTrailingZeros().scale() > 2
                || dto.valorBase().compareTo(new java.math.BigDecimal("99999999.99")) > 0) {
            throw new com.atelie.sistema.shared.exception.RegraNegocioException("Dados da peça inválidos.");
        }
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    public Peca buscarEntidadePorId(UUID id) {
        return pecaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + id));
    }

    @Transactional
    public PecaResponseDTO enviarParaCostura(UUID id) {
        Peca peca = buscarParaAtualizacao(id);
        peca.enviarParaCostura();
        return toResponseDTO(pecaRepository.save(peca));
    }

    @Transactional
    public PecaResponseDTO enviarParaLavagem(UUID id) {
        Peca peca = buscarParaAtualizacao(id);
        if (peca.getStatusDisponibilidade() == StatusDisponibilidade.ALUGADA) {
            throw new com.atelie.sistema.shared.exception.RegraNegocioException(
                    "Registre a devolução na ordem de serviço para encaminhar a peça à lavagem.");
        }
        peca.enviarParaLavagem();
        return toResponseDTO(pecaRepository.save(peca));
    }

    @Transactional
    public PecaResponseDTO devolverParaEstoque(UUID id, LiberacaoPecaRequestDTO confirmacao) {
        Peca peca = buscarParaAtualizacao(id);
        peca.devolverParaEstoque(confirmacao != null && Boolean.TRUE.equals(confirmacao.lavagemConcluida()),
                confirmacao != null && Boolean.TRUE.equals(confirmacao.condicoesConferidas()));
        return toResponseDTO(pecaRepository.save(peca));
    }

    private Peca buscarParaAtualizacao(UUID id) {
        return pecaRepository.buscarParaReserva(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + id));
    }

    private PecaResponseDTO toResponseDTO(Peca p) {
        return new PecaResponseDTO(p.getId(), p.getDescricao(), p.getTipo(), p.getStatusDisponibilidade(),
                p.getCodigoEtiqueta(), p.getTamanho(), p.getCor(), p.getValorBase());
    }
}
