package com.atelie.sistema.ordemservico;

import com.atelie.sistema.cliente.Cliente;
import com.atelie.sistema.cliente.ClienteService;
import com.atelie.sistema.ordemservico.dto.OrdemServicoRequestDTO;
import com.atelie.sistema.ordemservico.dto.OrdemServicoResponseDTO;
import com.atelie.sistema.ordemservico.dto.PagamentoResponse;
import com.atelie.sistema.ordemservico.dto.RegistrarPagamentoRequest;
import com.atelie.sistema.ordemservico.dto.ReagendarOrdemServicoRequest;
import com.atelie.sistema.ordemservico.dto.OrdemProducaoResponse;
import com.atelie.sistema.ordemservico.dto.ItemProducaoRequest;
import com.atelie.sistema.ordemservico.dto.CompromissoResponse;
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
        var detalhes = validarDetalhesItens(dto);
        List<ItemOrdemServico> itens = dto.pecas().stream().sorted().map(id -> {
            Peca peca = pecaRepository.buscarParaReserva(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + id));
            if (peca.getTipo() != dto.tipoFluxo()) {
                throw new RegraNegocioException("A peça precisa pertencer ao mesmo tipo da ordem.");
            }
            if (locacao) {
                validarDisponibilidade(peca, dto.inicioLocacao(), dto.fimLocacao());
            }
            ItemProducaoRequest detalhe = detalhes.get(id);
            return new ItemOrdemServico(id, peca.getValorBase(),
                    detalhe == null ? dto.medidas() : detalhe.medidas(),
                    detalhe == null ? dto.instrucoesCostura() : detalhe.instrucoesCostura());
        }).toList();
        BigDecimal subtotal = itens.stream().map(ItemOrdemServico::valorUnitario)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal desconto = dto.desconto() == null ? BigDecimal.ZERO : dto.desconto();
        if (dto.descontoPercentual() != null) {
            BigDecimal percentual = dto.descontoPercentual();
            if (dto.desconto() != null) {
                throw new RegraNegocioException("Informe apenas um desconto: em reais ou em porcentagem.");
            }
            if (percentual.signum() < 0 || percentual.compareTo(new BigDecimal("100")) >= 0
                    || percentual.stripTrailingZeros().scale() > 2) {
                throw new RegraNegocioException("Informe um desconto de 0% a 99,99%, com até duas casas decimais.");
            }
            desconto = subtotal.multiply(percentual).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        }
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

    private java.util.Map<UUID, ItemProducaoRequest> validarDetalhesItens(OrdemServicoRequestDTO dto) {
        if (dto.detalhesItens() == null || dto.detalhesItens().isEmpty()) return java.util.Map.of();
        var detalhes = new java.util.HashMap<UUID, ItemProducaoRequest>();
        for (ItemProducaoRequest detalhe : dto.detalhesItens()) {
            if (detalhe == null || detalhe.pecaId() == null || !dto.pecas().contains(detalhe.pecaId())
                    || detalhe.medidas() != null && detalhe.medidas().length() > 10000
                    || detalhe.instrucoesCostura() != null && detalhe.instrucoesCostura().length() > 10000
                    || detalhes.putIfAbsent(detalhe.pecaId(), detalhe) != null) {
                throw new RegraNegocioException("Informe dados de produção válidos para cada peça selecionada.");
            }
        }
        return detalhes;
    }

    @Transactional(readOnly = true)
    public boolean disponivelNoPeriodo(UUID pecaId, java.time.LocalDateTime inicio, java.time.LocalDateTime fim) {
        if (inicio == null || fim == null || !inicio.isBefore(fim)) {
            throw new RegraNegocioException("Informe um período de locação válido.");
        }
        Peca peca = pecaRepository.findById(pecaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + pecaId));
        if (peca.getTipo() != TipoFluxo.LOCACAO
                || ordemServicoRepository.contarReservas(pecaId, inicio, fim) > 0) return false;
        // Condição física atual só impede uma retirada que começaria agora/passado;
        // não bloqueia reservas futuras sem uma janela de manutenção definida.
        return inicio.isAfter(java.time.LocalDateTime.now())
                || peca.getStatusDisponibilidade() == com.atelie.sistema.peca.state.StatusDisponibilidade.DISPONIVEL;
    }

    private void validarDisponibilidade(Peca peca, java.time.LocalDateTime inicio, java.time.LocalDateTime fim) {
        if (ordemServicoRepository.contarReservas(peca.getId(), inicio, fim) > 0) {
            throw new RegraNegocioException("Peça já reservada no período: " + peca.getCodigoEtiqueta());
        }
        if (!inicio.isAfter(java.time.LocalDateTime.now())
                && peca.getStatusDisponibilidade() != com.atelie.sistema.peca.state.StatusDisponibilidade.DISPONIVEL) {
            throw new RegraNegocioException("A peça não está fisicamente disponível para uma locação com início imediato.");
        }
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponseDTO> listarTodas() {
        return ordemServicoRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponseDTO> listarPorCliente(UUID clienteId) {
        clienteService.buscarEntidadePorId(clienteId);
        return ordemServicoRepository.findByClienteId(clienteId).stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<OrdemProducaoResponse> listarFilaProducao() {
        return ordemServicoRepository.findAll().stream()
                .filter(os -> !List.of(StatusOrdemServico.CANCELADA, StatusOrdemServico.ENTREGUE,
                        StatusOrdemServico.DEVOLVIDA, StatusOrdemServico.RETIRADA).contains(os.getStatusAtual()))
                .map(this::toOrdemProducaoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdemProducaoResponse buscarParaProducao(UUID id) {
        OrdemServico os = buscarEntidadePorId(id);
        if (List.of(StatusOrdemServico.CANCELADA, StatusOrdemServico.ENTREGUE,
                StatusOrdemServico.DEVOLVIDA, StatusOrdemServico.RETIRADA).contains(os.getStatusAtual())) {
            throw new RegraNegocioException("Esta ordem não está mais na fila de produção.");
        }
        return toOrdemProducaoResponse(os);
    }

    private OrdemProducaoResponse toOrdemProducaoResponse(OrdemServico os) {
        return new OrdemProducaoResponse(os.getId(), os.getCliente().getNome(), os.getTipoFluxo(),
                os.getStatusAtual(), os.getDataEntrega(), os.getInicioLocacao(), os.getDataProva(),
                os.getDataEvento(), os.getMedidas(), os.getInstrucoesCostura(),
                os.getItens().stream().map(item -> new OrdemProducaoResponse.Item(item.pecaId(),
                        item.medidas(), item.instrucoesCostura())).toList());
    }

    @Transactional(readOnly = true)
    public List<CompromissoResponse> compromissosDoDia(java.time.LocalDate data) {
        if (data == null) throw new RegraNegocioException("Informe uma data válida para a agenda.");
        return ordemServicoRepository.findAll().stream()
                .filter(os -> os.getStatusAtual() != StatusOrdemServico.CANCELADA)
                .flatMap(os -> {
                    var compromissos = new java.util.ArrayList<CompromissoResponse>();
                    var status = os.getStatusAtual();
                    boolean emProducao = !List.of(StatusOrdemServico.RETIRADA, StatusOrdemServico.ENTREGUE,
                            StatusOrdemServico.DEVOLVIDA, StatusOrdemServico.CANCELADA).contains(status);
                    if (emProducao && os.getDataProva() != null && os.getDataProva().toLocalDate().equals(data))
                        compromissos.add(compromisso(os, "Prova", os.getDataProva()));
                    if (os.getTipoFluxo() == TipoFluxo.LOCACAO) {
                        if (status == StatusOrdemServico.CONCLUIDO && os.getInicioLocacao() != null
                                && os.getInicioLocacao().toLocalDate().equals(data))
                            compromissos.add(compromisso(os, "Retirada", os.getInicioLocacao()));
                        if (status == StatusOrdemServico.RETIRADA && os.getFimLocacao() != null
                                && os.getFimLocacao().toLocalDate().equals(data))
                            compromissos.add(compromisso(os, "Devolução", os.getFimLocacao()));
                    } else if (status != StatusOrdemServico.ENTREGUE && os.getDataEntrega() != null
                            && os.getDataEntrega().toLocalDate().equals(data)) {
                        compromissos.add(compromisso(os, "Entrega", os.getDataEntrega()));
                    }
                    return compromissos.stream();
                })
                .sorted(java.util.Comparator.comparing(CompromissoResponse::dataHora))
                .toList();
    }

    private CompromissoResponse compromisso(OrdemServico os, String tipo, java.time.LocalDateTime dataHora) {
        return new CompromissoResponse(os.getId(), os.getCliente().getNome(), os.getTipoFluxo(), tipo, dataHora, os.getStatusAtual());
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
        OrdemServico os = ordemServicoRepository.buscarParaAtualizacao(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço não encontrada com id: " + id));
        os.cancelar();
        return toResponseDTO(os);
    }

    @Transactional
    public OrdemServicoResponseDTO reagendar(UUID id, ReagendarOrdemServicoRequest request) {
        OrdemServico os = ordemServicoRepository.buscarParaAtualizacao(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço não encontrada com id: " + id));
        if (request == null) throw new RegraNegocioException("Informe as novas datas da ordem.");
        if (os.getTipoFluxo() == TipoFluxo.LOCACAO
                && (request.inicioLocacao() == null || request.fimLocacao() == null
                    || !request.inicioLocacao().isBefore(request.fimLocacao()))) {
            throw new RegraNegocioException("Informe um período de locação válido.");
        }
        if (os.getTipoFluxo() == TipoFluxo.LOCACAO) {
            for (UUID pecaId : os.getItens().stream().map(ItemOrdemServico::pecaId).sorted().toList()) {
                Peca peca = pecaRepository.buscarParaReserva(pecaId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + pecaId));
                if (peca.getTipo() != TipoFluxo.LOCACAO
                        || ordemServicoRepository.contarReservasDeOutrasOrdens(pecaId, request.inicioLocacao(), request.fimLocacao(), id) > 0) {
                    throw new RegraNegocioException("A nova data conflita com outra reserva da peça " + peca.getCodigoEtiqueta() + ".");
                }
                if (!request.inicioLocacao().isAfter(java.time.LocalDateTime.now())
                        && peca.getStatusDisponibilidade() != com.atelie.sistema.peca.state.StatusDisponibilidade.DISPONIVEL) {
                    throw new RegraNegocioException("A peça não está fisicamente disponível para uma locação com início imediato.");
                }
            }
        }
        os.reagendar(request.inicioLocacao(), request.fimLocacao(), request.dataProva(), request.dataEvento(), request.dataEntrega());
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
        OrdemServico os = ordemServicoRepository.buscarParaAtualizacao(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço não encontrada com id: " + id));
        os.retirar();
        if (os.getTipoFluxo() == TipoFluxo.LOCACAO) {
            if (os.getItens().isEmpty() || os.getInicioLocacao() == null || os.getFimLocacao() == null
                    || !os.getInicioLocacao().isBefore(os.getFimLocacao())) {
                throw new RegraNegocioException("A ordem não possui uma reserva válida para retirada.");
            }
            // Mesma ordem de bloqueio usada na reserva, inclusive em ordens com várias peças.
            for (UUID pecaId : os.getItens().stream().map(ItemOrdemServico::pecaId).sorted().toList()) {
                Peca peca = pecaRepository.buscarParaReserva(pecaId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + pecaId));
                if (ordemServicoRepository.contarReservas(pecaId, os.getInicioLocacao(), os.getFimLocacao()) != 1) {
                    throw new RegraNegocioException("A reserva da peça está inconsistente. Confira as ordens antes da retirada.");
                }
                peca.retirarParaLocacao();
            }
        }
        return toResponseDTO(os);
    }

    @Transactional
    public OrdemServicoResponseDTO devolver(UUID id) {
        OrdemServico os = ordemServicoRepository.buscarParaAtualizacao(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço não encontrada com id: " + id));
        os.devolver();
        if (os.getItens().isEmpty()) {
            throw new RegraNegocioException("A ordem não possui peças para devolução.");
        }
        for (UUID pecaId : os.getItens().stream().map(ItemOrdemServico::pecaId).sorted().toList()) {
            Peca peca = pecaRepository.buscarParaReserva(pecaId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + pecaId));
            peca.devolverDaLocacao();
        }
        return toResponseDTO(os);
    }

    @Transactional
    public PagamentoResponse registrarSinal(UUID id, RegistrarPagamentoRequest request) {
        OrdemServico os = buscarEntidadePorId(id);
        validarOrdemFinanceiraAtiva(os);
        os.getPagamento().registrarRecebimento(request.valor(), request.data(), request.metodo(), true);
        return toPagamentoResponse(os.getPagamento());
    }

    @Transactional
    public PagamentoResponse registrarPagamento(UUID id, RegistrarPagamentoRequest request) {
        OrdemServico os = buscarEntidadePorId(id);
        validarOrdemFinanceiraAtiva(os);
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
                        .map(r -> new PagamentoResponse.Movimento(r.getId(), r.getValor(), r.getData(), r.getMetodo(), r.isSinal()))
                        .toList());
    }

    private void validarOrdemFinanceiraAtiva(OrdemServico os) {
        if (os.getStatusAtual() == StatusOrdemServico.CANCELADA) {
            throw new RegraNegocioException("Não é possível registrar pagamentos em uma ordem cancelada; consulte a pendência de acerto.");
        }
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
                os.getFimLocacao(), os.getDataProva(), os.getDataEvento(), os.getInstrucoesCostura(), os.getMedidas(), os.isPendenciaAcerto());
    }
}
