package com.atelie.sistema.peca;

import com.atelie.sistema.peca.dto.LiberacaoPecaRequestDTO;
import com.atelie.sistema.peca.dto.PecaProducaoResponse;
import com.atelie.sistema.peca.dto.PecaRequestDTO;
import com.atelie.sistema.peca.dto.PecaResponseDTO;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PecaService {

    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("99999999.99");

    private final PecaRepository pecaRepository;

    public PecaService(PecaRepository pecaRepository) {
        this.pecaRepository = pecaRepository;
    }

    @Transactional
    public PecaResponseDTO cadastrarPeca(PecaRequestDTO dto) {
        validarDados(dto, true);
        Peca novaPeca = new Peca(dto.descricao().strip(), dto.tipo(), dto.valorBase(),
                normalizarOpcional(dto.tamanho()), normalizarOpcional(dto.cor()));
        return toResponseDTO(pecaRepository.save(novaPeca));
    }

    @Transactional(readOnly = true)
    public List<PecaResponseDTO> listarTodas() {
        return pecaRepository.findAll().stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<PecaProducaoResponse> listarParaProducao() {
        return pecaRepository.findAll().stream().map(this::toProducaoResponse).toList();
    }

    @Transactional(readOnly = true)
    public PecaProducaoResponse buscarParaProducao(UUID id) {
        return toProducaoResponse(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
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

    @Transactional
    public PecaResponseDTO enviarParaCostura(UUID id) {
        Peca peca = buscarParaAtualizacao(id);
        peca.enviarParaCostura();
        return toResponseDTO(peca);
    }

    @Transactional
    public PecaResponseDTO enviarParaLavagem(UUID id) {
        Peca peca = buscarParaAtualizacao(id);
        peca.enviarParaLavagem();
        return toResponseDTO(peca);
    }

    @Transactional
    public PecaResponseDTO devolverParaEstoque(UUID id, LiberacaoPecaRequestDTO confirmacao) {
        Peca peca = buscarParaAtualizacao(id);
        peca.devolverParaEstoque(confirmacao != null && Boolean.TRUE.equals(confirmacao.lavagemConcluida()),
                confirmacao != null && Boolean.TRUE.equals(confirmacao.condicoesConferidas()));
        return toResponseDTO(peca);
    }

    public Peca buscarEntidadePorId(UUID id) {
        return pecaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + id));
    }

    private void validarDados(PecaRequestDTO dto, boolean cadastro) {
        BigDecimal valor = dto == null ? null : dto.valorBase();
        boolean valorInvalido = valor == null ? !cadastro
                : valor.signum() < 0 || valor.stripTrailingZeros().scale() > 2 || valor.compareTo(VALOR_MAXIMO) > 0;
        if (dto == null || (cadastro && dto.tipo() == null) || dto.descricao() == null || dto.descricao().isBlank()
                || dto.descricao().length() > 255
                || (dto.tamanho() != null && dto.tamanho().length() > 10)
                || (dto.cor() != null && dto.cor().length() > 50)
                || valorInvalido) {
            throw new RegraNegocioException("Dados da peça inválidos.");
        }
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private Peca buscarParaAtualizacao(UUID id) {
        return pecaRepository.buscarParaReserva(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + id));
    }

    private PecaResponseDTO toResponseDTO(Peca p) {
        return new PecaResponseDTO(p.getId(), p.getDescricao(), p.getTipo(), p.getStatusDisponibilidade(),
                p.getCodigoEtiqueta(), p.getTamanho(), p.getCor(), p.getValorBase());
    }

    private PecaProducaoResponse toProducaoResponse(Peca p) {
        return new PecaProducaoResponse(p.getId(), p.getDescricao(), p.getCodigoEtiqueta(),
                p.getTamanho(), p.getCor(), p.getStatusDisponibilidade());
    }
}
