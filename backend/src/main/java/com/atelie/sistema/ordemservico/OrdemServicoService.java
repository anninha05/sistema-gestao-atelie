package com.atelie.sistema.ordemservico;

import com.atelie.sistema.cliente.Cliente;
import com.atelie.sistema.cliente.ClienteService;
import com.atelie.sistema.ordemservico.dto.AcaoResponse;
import com.atelie.sistema.ordemservico.dto.CompromissoResponse;
import com.atelie.sistema.ordemservico.dto.ItemProducaoRequest;
import com.atelie.sistema.ordemservico.dto.OrdemProducaoResponse;
import com.atelie.sistema.ordemservico.dto.OrdemServicoRequestDTO;
import com.atelie.sistema.ordemservico.dto.OrdemServicoResponseDTO;
import com.atelie.sistema.ordemservico.dto.PagamentoResponse;
import com.atelie.sistema.ordemservico.dto.ReagendarOrdemServicoRequest;
import com.atelie.sistema.ordemservico.dto.RegistrarPagamentoRequest;
import com.atelie.sistema.ordemservico.state.AcaoOrdemServico;
import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.peca.Peca;
import com.atelie.sistema.peca.PecaRepository;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.peca.state.StatusDisponibilidade;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdemServicoService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final ClienteService clienteService;
    private final PecaRepository pecaRepository;

    public OrdemServicoService(OrdemServicoRepository ordemServicoRepository, ClienteService clienteService,
            PecaRepository pecaRepository) {
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
        if (locacao) {
            validarPeriodo(dto.inicioLocacao(), dto.fimLocacao());
        }
        var detalhes = validarDetalhesItens(dto);
        List<ItemOrdemServico> itens = dto.pecas().stream().sorted().map(id -> {
            Peca peca = bloquearPeca(id);
            if (peca.getTipo() != dto.tipoFluxo()) {
                throw new RegraNegocioException("A peça precisa pertencer ao mesmo tipo da ordem.");
            }
            if (locacao) {
                if (ordemServicoRepository.contarReservas(id, dto.inicioLocacao(), dto.fimLocacao()) > 0) {
                    throw new RegraNegocioException("Peça já reservada no período: " + peca.getCodigoEtiqueta());
                }
                validarProntidaoFisica(peca, dto.inicioLocacao());
            }
            ItemProducaoRequest detalhe = detalhes.get(id);
            return new ItemOrdemServico(id, peca.getValorBase(),
                    detalhe == null ? dto.medidas() : detalhe.medidas(),
                    detalhe == null ? dto.instrucoesCostura() : detalhe.instrucoesCostura());
        }).toList();
        BigDecimal subtotal = itens.stream().map(ItemOrdemServico::valorUnitario)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal desconto = calcularDesconto(dto, subtotal);
        BigDecimal total = subtotal.subtract(desconto).setScale(2, RoundingMode.UNNECESSARY);
        if (total.signum() <= 0 || total.precision() > 10) {
            throw new RegraNegocioException("O valor da ordem deve ser positivo e ter até oito dígitos inteiros.");
        }
        OrdemServico os = new OrdemServico(cliente, dto.tipoFluxo(), total, dto.dataEntrega(),
                itens, desconto, locacao ? dto.inicioLocacao() : null, locacao ? dto.fimLocacao() : null,
                dto.dataProva(), dto.dataEvento(), dto.instrucoesCostura(), dto.medidas());
        return toResponseDTO(ordemServicoRepository.save(os));
    }

    private BigDecimal calcularDesconto(OrdemServicoRequestDTO dto, BigDecimal subtotal) {
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
        return desconto;
    }

    private Map<UUID, ItemProducaoRequest> validarDetalhesItens(OrdemServicoRequestDTO dto) {
        if (dto.detalhesItens() == null || dto.detalhesItens().isEmpty()) {
            return Map.of();
        }
        var detalhes = new HashMap<UUID, ItemProducaoRequest>();
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
    public boolean disponivelNoPeriodo(UUID pecaId, LocalDateTime inicio, LocalDateTime fim) {
        validarPeriodo(inicio, fim);
        Peca peca = pecaRepository.findById(pecaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + pecaId));
        if (peca.getTipo() != TipoFluxo.LOCACAO || ordemServicoRepository.contarReservas(pecaId, inicio, fim) > 0) {
            return false;
        }
        return comecaNoFuturo(inicio) || peca.getStatusDisponibilidade() == StatusDisponibilidade.DISPONIVEL;
    }

    private void validarPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null || !inicio.isBefore(fim)) {
            throw new RegraNegocioException("Informe um período de locação válido.");
        }
    }

    private void validarProntidaoFisica(Peca peca, LocalDateTime inicio) {
        if (!comecaNoFuturo(inicio) && peca.getStatusDisponibilidade() != StatusDisponibilidade.DISPONIVEL) {
            throw new RegraNegocioException("A peça não está fisicamente disponível para uma locação com início imediato.");
        }
    }

    private boolean comecaNoFuturo(LocalDateTime inicio) {
        return inicio.isAfter(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponseDTO> listarTodas() {
        return ordemServicoRepository.findAll().stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponseDTO> listarPorCliente(UUID clienteId) {
        clienteService.buscarEntidadePorId(clienteId);
        return ordemServicoRepository.findByClienteId(clienteId).stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public OrdemServicoResponseDTO buscarPorId(UUID id) {
        return toResponseDTO(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public List<OrdemProducaoResponse> listarFilaProducao() {
        return ordemServicoRepository.findAll().stream()
                .filter(OrdemServico::estaEmProducao)
                .map(this::toOrdemProducaoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdemProducaoResponse buscarParaProducao(UUID id) {
        OrdemServico os = buscarEntidadePorId(id);
        if (!os.estaEmProducao()) {
            throw new RegraNegocioException("Esta ordem não está mais na fila de produção.");
        }
        return toOrdemProducaoResponse(os);
    }

    @Transactional(readOnly = true)
    public List<CompromissoResponse> compromissosDoDia(LocalDate data) {
        if (data == null) {
            throw new RegraNegocioException("Informe uma data válida para a agenda.");
        }
        return ordemServicoRepository.findAll().stream()
                .flatMap(os -> compromissosDaOrdem(os, data).stream())
                .sorted(Comparator.comparing(CompromissoResponse::dataHora))
                .toList();
    }

    private List<CompromissoResponse> compromissosDaOrdem(OrdemServico os, LocalDate data) {
        var compromissos = new ArrayList<CompromissoResponse>();
        StatusOrdemServico status = os.getStatusAtual();
        if (os.estaEmProducao() && noDia(os.getDataProva(), data)) {
            compromissos.add(compromisso(os, "Prova", os.getDataProva()));
        }
        if (os.getTipoFluxo() == TipoFluxo.LOCACAO) {
            if (status == StatusOrdemServico.CONCLUIDO && noDia(os.getInicioLocacao(), data)) {
                compromissos.add(compromisso(os, "Retirada", os.getInicioLocacao()));
            }
            if (status == StatusOrdemServico.RETIRADA && noDia(os.getFimLocacao(), data)) {
                compromissos.add(compromisso(os, "Devolução", os.getFimLocacao()));
            }
        } else if (os.estaEmProducao() && noDia(os.getDataEntrega(), data)) {
            compromissos.add(compromisso(os, "Entrega", os.getDataEntrega()));
        }
        return compromissos;
    }

    private boolean noDia(LocalDateTime dataHora, LocalDate dia) {
        return dataHora != null && dataHora.toLocalDate().equals(dia);
    }

    private CompromissoResponse compromisso(OrdemServico os, String tipo, LocalDateTime dataHora) {
        return new CompromissoResponse(os.getId(), os.getCliente().getNome(), os.getTipoFluxo(), tipo, dataHora,
                os.getStatusAtual());
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
    public OrdemServicoResponseDTO concluir(UUID id) {
        OrdemServico os = buscarEntidadePorId(id);
        os.concluir();
        return toResponseDTO(os);
    }

    @Transactional
    public OrdemServicoResponseDTO cancelar(UUID id) {
        OrdemServico os = bloquearOrdem(id);
        os.cancelar();
        return toResponseDTO(os);
    }

    @Transactional
    public OrdemServicoResponseDTO reagendar(UUID id, ReagendarOrdemServicoRequest request) {
        OrdemServico os = bloquearOrdem(id);
        if (request == null) {
            throw new RegraNegocioException("Informe as novas datas da ordem.");
        }
        os.getEstado().validarReagendamento();
        if (os.getTipoFluxo() == TipoFluxo.LOCACAO) {
            validarPeriodo(request.inicioLocacao(), request.fimLocacao());
            for (UUID pecaId : idsDasPecas(os)) {
                Peca peca = bloquearPeca(pecaId);
                if (peca.getTipo() != TipoFluxo.LOCACAO || ordemServicoRepository.contarReservasDeOutrasOrdens(
                        pecaId, request.inicioLocacao(), request.fimLocacao(), id) > 0) {
                    throw new RegraNegocioException(
                            "A nova data conflita com outra reserva da peça " + peca.getCodigoEtiqueta() + ".");
                }
                validarProntidaoFisica(peca, request.inicioLocacao());
            }
        }
        os.reagendar(request.inicioLocacao(), request.fimLocacao(), request.dataProva(), request.dataEvento(),
                request.dataEntrega());
        return toResponseDTO(os);
    }

    @Transactional
    public OrdemServicoResponseDTO retirar(UUID id) {
        OrdemServico os = bloquearOrdem(id);
        os.retirar();
        if (os.getTipoFluxo() == TipoFluxo.LOCACAO) {
            if (os.getItens().isEmpty() || os.getInicioLocacao() == null || os.getFimLocacao() == null
                    || !os.getInicioLocacao().isBefore(os.getFimLocacao())) {
                throw new RegraNegocioException("A ordem não possui uma reserva válida para retirada.");
            }
            for (UUID pecaId : idsDasPecas(os)) {
                Peca peca = bloquearPeca(pecaId);
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
        OrdemServico os = bloquearOrdem(id);
        os.devolver();
        if (os.getItens().isEmpty()) {
            throw new RegraNegocioException("A ordem não possui peças para devolução.");
        }
        idsDasPecas(os).stream().map(this::bloquearPeca).forEach(Peca::devolverDaLocacao);
        return toResponseDTO(os);
    }

    private OrdemServico bloquearOrdem(UUID id) {
        return ordemServicoRepository.buscarParaAtualizacao(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço não encontrada com id: " + id));
    }

    private List<UUID> idsDasPecas(OrdemServico os) {
        return os.getItens().stream().map(ItemOrdemServico::pecaId).sorted().toList();
    }

    private Peca bloquearPeca(UUID pecaId) {
        return pecaRepository.buscarParaReserva(pecaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + pecaId));
    }

    @Transactional
    public PagamentoResponse registrarSinal(UUID id, RegistrarPagamentoRequest request) {
        return registrarRecebimento(id, request, true);
    }

    @Transactional
    public PagamentoResponse registrarPagamento(UUID id, RegistrarPagamentoRequest request) {
        return registrarRecebimento(id, request, false);
    }

    private PagamentoResponse registrarRecebimento(UUID id, RegistrarPagamentoRequest request, boolean sinal) {
        if (request == null) {
            throw new RegraNegocioException("Informe o valor, a data e o método do recebimento.");
        }
        OrdemServico os = buscarEntidadePorId(id);
        os.registrarRecebimento(request.valor(), request.data(), request.metodo(), sinal);
        return toPagamentoResponse(os.getPagamento());
    }

    @Transactional(readOnly = true)
    public PagamentoResponse buscarPagamento(UUID id) {
        return toPagamentoResponse(buscarEntidadePorId(id).getPagamento());
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
                os.getFimLocacao(), os.getDataProva(), os.getDataEvento(), os.getInstrucoesCostura(), os.getMedidas(),
                os.isPendenciaAcerto(),
                os.acoesDisponiveis().stream().map(AcaoResponse::de).toList());
    }

    private OrdemProducaoResponse toOrdemProducaoResponse(OrdemServico os) {
        return new OrdemProducaoResponse(os.getId(), os.getCliente().getNome(), os.getTipoFluxo(),
                os.getStatusAtual(), os.getDataEntrega(), os.getInicioLocacao(), os.getDataProva(),
                os.getDataEvento(), os.getMedidas(), os.getInstrucoesCostura(),
                os.getItens().stream().map(item -> new OrdemProducaoResponse.Item(item.pecaId(),
                        item.medidas(), item.instrucoesCostura())).toList(),
                os.acoesDisponiveis().stream()
                        .filter(acao -> acao.acao().getSetor() == AcaoOrdemServico.Setor.PRODUCAO)
                        .map(AcaoResponse::de).toList());
    }
}
