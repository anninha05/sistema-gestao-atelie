package com.atelie.sistema.ordemservico;

import com.atelie.sistema.cliente.Cliente;
import com.atelie.sistema.cliente.ClienteService;
import com.atelie.sistema.ordemservico.dto.OrdemServicoRequestDTO;
import com.atelie.sistema.ordemservico.dto.OrdemServicoResponseDTO;
import com.atelie.sistema.ordemservico.dto.PagamentoResponse;
import com.atelie.sistema.ordemservico.dto.RegistrarPagamentoRequest;
import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.peca.Peca;
import com.atelie.sistema.peca.PecaRepository;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrdemServicoService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final ClienteService clienteService;
    private final PecaRepository pecaRepository;

    public OrdemServicoService(OrdemServicoRepository ordemServicoRepository, ClienteService clienteService, PecaRepository pecaRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.clienteService = clienteService;
        this.pecaRepository = pecaRepository;
    }

    @Transactional
    public OrdemServicoResponseDTO abrirOrdemServico(OrdemServicoRequestDTO dto) {
        if (dto.clienteId() == null) {
            throw new RegraNegocioException("Informe o cliente da ordem.");
        }
        Cliente cliente = clienteService.buscarEntidadePorId(dto.clienteId());

        if (dto.tipoFluxo() == null || dto.pecas() == null || dto.pecas().isEmpty()
                || dto.pecas().stream().anyMatch(Objects::isNull)
                || dto.pecas().stream().distinct().count() != dto.pecas().size()) {
            throw new RegraNegocioException("Informe o tipo da ordem e pelo menos uma peça, sem repetições.");
        }
        boolean locacao = dto.tipoFluxo() == TipoFluxo.LOCACAO;
        if (locacao && (dto.inicioLocacao() == null || dto.fimLocacao() == null
                || !dto.inicioLocacao().isBefore(dto.fimLocacao()))) {
            throw new RegraNegocioException("Informe um período de locação válido.");
        }
        List<ItemOrdemServico> itens = dto.pecas().stream().sorted().map(id -> {
            Peca peca = pecaRepository.buscarParaReserva(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + id));
            if (peca.getTipo() != dto.tipoFluxo()) {
                throw new RegraNegocioException("A peça precisa pertencer ao mesmo tipo da ordem.");
            }
            if (locacao && ordemServicoRepository.contarReservas(id, dto.inicioLocacao(), dto.fimLocacao()) > 0) {
                throw new RegraNegocioException("Peça já reservada no período: " + id);
            }
            return new ItemOrdemServico(id, peca.getValorBase());
        }).toList();
        BigDecimal subtotal = itens.stream().map(ItemOrdemServico::valorUnitario)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal desconto = dto.desconto() == null ? BigDecimal.ZERO : dto.desconto();
        if (desconto.signum() < 0 || desconto.stripTrailingZeros().scale() > 2
                || desconto.compareTo(subtotal) > 0) {
            throw new RegraNegocioException("Desconto inválido para o subtotal da ordem.");
        }
        BigDecimal total = subtotal.subtract(desconto).setScale(2, RoundingMode.UNNECESSARY);
        if (total.signum() <= 0 || total.precision() > 10) {
            throw new RegraNegocioException("O valor da ordem deve ser positivo e ter até oito dígitos inteiros.");
        }
        OrdemServico os = new OrdemServico(cliente, dto.tipoFluxo(), total, dto.dataEntrega(),
                itens, desconto, locacao ? dto.inicioLocacao() : null, locacao ? dto.fimLocacao() : null,
                dto.dataProva(), dto.dataEvento(), dto.instrucoesCostura(), dto.medidas());

        OrdemServico salva = ordemServicoRepository.save(os);
        return toResponseDTO(salva);
    }

    @Transactional(readOnly = true)
    public boolean disponivelNoPeriodo(UUID pecaId, java.time.LocalDateTime inicio, java.time.LocalDateTime fim) {
        if (inicio == null || fim == null || !inicio.isBefore(fim)) {
            throw new RegraNegocioException("Informe um período de locação válido.");
        }
        Peca peca = pecaRepository.findById(pecaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + pecaId));
        return peca.getTipo() == TipoFluxo.LOCACAO && ordemServicoRepository.contarReservas(pecaId, inicio, fim) == 0;
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponseDTO> listarTodas() {
        return ordemServicoRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdemServicoResponseDTO buscarPorId(UUID id) {
        return toResponseDTO(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponseDTO> listarPorStatus(StatusOrdemServico status) {
        return ordemServicoRepository.findByStatusAtual(status).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public OrdemServico buscarEntidadePorId(UUID id) {
        return ordemServicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço não encontrada com id: " + id));
    }

    @Transactional
    public OrdemServicoResponseDTO iniciarConfeccao(UUID id) {
        OrdemServico os = buscarEntidadePorId(id);
        os.iniciarConfeccao();
        return toResponseDTO(os);
    }

    @Transactional
    public OrdemServicoResponseDTO enviarParaProva(UUID id) {
        OrdemServico os = buscarEntidadePorId(id);
        os.enviarParaProva();
        return toResponseDTO(os);
    }

    @Transactional
    public OrdemServicoResponseDTO iniciarPassadoria(UUID id) {
        OrdemServico os = buscarEntidadePorId(id);
        os.iniciarPassadoria();
        return toResponseDTO(os);
    }

    @Transactional
    public OrdemServicoResponseDTO cancelar(UUID id) {
        OrdemServico os = buscarEntidadePorId(id);
        os.cancelar();
        return toResponseDTO(os);
    }

    @Transactional
    public OrdemServicoResponseDTO concluir(UUID id) {
        OrdemServico os = buscarEntidadePorId(id);
        os.concluir();
        return toResponseDTO(os);
    }

    @Transactional
    public OrdemServicoResponseDTO retirar(UUID id) {
        OrdemServico os = buscarEntidadePorId(id);
        os.retirar();
        return toResponseDTO(os);
    }

    @Transactional
    public OrdemServicoResponseDTO devolver(UUID id) {
        OrdemServico os = buscarEntidadePorId(id);
        os.devolver();
        return toResponseDTO(os);
    }

    @Transactional
    public PagamentoResponse registrarSinal(UUID id, RegistrarPagamentoRequest request) {
        OrdemServico os = buscarEntidadePorId(id);
        os.getPagamento().registrarRecebimento(request.valor(), request.data(), request.metodo(), true);
        return toPagamentoResponse(os.getPagamento());
    }

    @Transactional
    public PagamentoResponse registrarPagamento(UUID id, RegistrarPagamentoRequest request) {
        OrdemServico os = buscarEntidadePorId(id);
        os.getPagamento().registrarRecebimento(request.valor(), request.data(), request.metodo(), false);
        return toPagamentoResponse(os.getPagamento());
    }

    @Transactional(readOnly = true)
    public PagamentoResponse buscarPagamento(UUID id) {
        OrdemServico os = buscarEntidadePorId(id);
        return toPagamentoResponse(os.getPagamento());
    }

    private PagamentoResponse toPagamentoResponse(Pagamento pagamento) {
        return new PagamentoResponse(
                pagamento.getValorTotal(),
                pagamento.getValorSinalPago(),
                pagamento.getValorPago(),
                pagamento.getSaldoDevedor(),
                pagamento.getStatusFinanceiro(),
                pagamento.getRecebimentos().stream()
                        .map(r -> new PagamentoResponse.Movimento(r.getValor(), r.getData(), r.getMetodo(), r.isSinal()))
                        .toList());
    }

    private OrdemServicoResponseDTO toResponseDTO(OrdemServico os) {
        return new OrdemServicoResponseDTO(
                os.getId(),
                os.getCliente().getId(),
                os.getCliente().getNome(),
                os.getTipoFluxo(),
                os.getStatusAtual(),
                os.getValorTotal(),
                os.getDataEntrega(),
                os.getCriadoEm(), os.getItens(), os.getDesconto(), os.getInicioLocacao(),
                os.getFimLocacao(), os.getDataProva(), os.getDataEvento(), os.getInstrucoesCostura(), os.getMedidas());
    }
}
