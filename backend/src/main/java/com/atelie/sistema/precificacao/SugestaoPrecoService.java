package com.atelie.sistema.precificacao;

import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.ordemservico.OrdemServicoRepository;
import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.precificacao.dto.SugestaoPrecoRequest;
import com.atelie.sistema.precificacao.dto.SugestaoPrecoResponse;
import com.atelie.sistema.shared.exception.RecursoNaoEncontradoException;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import com.atelie.sistema.usuario.Usuario;
import com.atelie.sistema.usuario.UsuarioRepository;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SugestaoPrecoService {
    private final OrdemServicoRepository ordens;
    private final ExecucaoPIDDiariaRepository execucoes;
    private final PoliticaPrecificacaoRepository politicas;
    private final SugestaoPrecoRepository sugestoes;
    private final UsuarioRepository usuarios;
    private final CalculadoraSugestaoPreco calculadora;
    private final PisoPrecoOrdemRepository pisos;

    SugestaoPrecoService(OrdemServicoRepository ordens, ExecucaoPIDDiariaRepository execucoes,
            PoliticaPrecificacaoRepository politicas, SugestaoPrecoRepository sugestoes,
            UsuarioRepository usuarios, CalculadoraSugestaoPreco calculadora, PisoPrecoOrdemRepository pisos) {
        this.ordens = ordens;
        this.execucoes = execucoes;
        this.politicas = politicas;
        this.sugestoes = sugestoes;
        this.usuarios = usuarios;
        this.calculadora = calculadora;
        this.pisos = pisos;
    }

    @Transactional(readOnly = true)
    SugestaoPrecoResponse simular(SugestaoPrecoRequest request, String email) {
        return calcular(request, email, false);
    }

    @Transactional
    SugestaoPrecoResponse gerarSugestao(SugestaoPrecoRequest request, String email) {
        return calcular(request, email, true);
    }

    @Transactional(readOnly = true)
    SugestaoPrecoResponse buscar(Long id) {
        return sugestoes.findById(id).map(this::resposta).orElseThrow(() ->
                new RecursoNaoEncontradoException("Sugestão de preço não encontrada."));
    }

    private SugestaoPrecoResponse calcular(SugestaoPrecoRequest request, String email, boolean persistir) {
        if (request == null || request.ordemServicoId() == null)
            throw new RegraNegocioException("Selecione a ordem para calcular uma sugestão de preço.");
        OrdemServico ordem = ordens.findById(request.ordemServicoId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Ordem de serviço não encontrada."));
        if (ordem.getStatusAtual() != StatusOrdemServico.RESERVADA)
            throw new RegraNegocioException("A sugestão só pode ser criada antes do início da produção ou entrega.");

        PoliticaPrecificacao politica = politicas.findById(PoliticaPrecificacao.ID_UNICA).orElseThrow(() ->
                new RecursoNaoEncontradoException("Configure a política de precificação antes de gerar sugestões."));
        Usuario responsavel = usuarios.findByEmail(email).orElseThrow(() ->
                new RecursoNaoEncontradoException("Usuário responsável não encontrado."));
        YearMonth mesAtual = YearMonth.now();
        ExecucaoPIDDiaria execucao = execucoes.findByCompetenciaOrderByDataApuracaoDesc(mesAtual.atDay(1))
                .stream().findFirst().orElse(null);
        double ajuste = execucao == null ? 0 : execucao.getAjusteLimitado();

        List<OrdemServico> historico = ordens.findByClienteId(ordem.getCliente().getId());
        int concluidas = (int) historico.stream().filter(os -> os.getStatusAtual() == StatusOrdemServico.ENTREGUE
                || os.getStatusAtual() == StatusOrdemServico.DEVOLVIDA).count();
        int canceladas = (int) historico.stream().filter(os -> os.getStatusAtual() == StatusOrdemServico.CANCELADA).count();
        var pisoCadastrado = pisos.findById(ordem.getId()).map(PisoPrecoOrdem::getValor).orElse(null);
        var piso = request.pisoMinimo() == null ? pisoCadastrado : request.pisoMinimo();
        if (pisoCadastrado != null && piso != null && piso.compareTo(pisoCadastrado) < 0)
            throw new RegraNegocioException("A sugestão deve respeitar o preço mínimo cadastrado para a ordem.");
        CalculoSugestaoPreco calculo = calculadora.calcular(ordem.getValorTotal(), ajuste,
                politica.getDescontoFidelidade(), politica.getQuantidadeOsFidelidade(), concluidas, canceladas,
                piso, politica.getAjusteMinimo(), politica.getAjusteMaximo());
        if (!persistir) return resposta(null, ordem.getId(), ordem.getCliente().getId(), execucao, calculo, responsavel, null);

        SugestaoPreco sugestao = sugestoes.save(new SugestaoPreco(ordem.getId(), ordem.getCliente().getId(),
                execucao == null ? null : execucao.getId(), calculo, responsavel));
        return resposta(sugestao.getId(), ordem.getId(), ordem.getCliente().getId(), execucao, calculo,
                responsavel, sugestao.getCriadaEm());
    }

    private SugestaoPrecoResponse resposta(SugestaoPreco sugestao) {
        return new SugestaoPrecoResponse(sugestao.getId(), sugestao.getOrdemServicoId(), sugestao.getClienteId(),
                sugestao.getExecucaoPidId(), sugestao.getPrecoBase(), sugestao.getAjustePid(),
                sugestao.getPrecoAjustado(), sugestao.getDescontoFidelidade(), sugestao.getPrecoAposFidelidade(),
                sugestao.getPisoMinimo(), sugestao.getPrecoSugerido(), sugestao.getOrdensConcluidas(),
                sugestao.getOrdensCanceladas(), sugestao.isElegivelFidelidade(), sugestao.getJustificativa(),
                sugestao.getResponsavel().getId(), sugestao.getResponsavel().getNome(), sugestao.getCriadaEm());
    }

    private SugestaoPrecoResponse resposta(Long id, UUID ordemId, UUID clienteId, ExecucaoPIDDiaria execucao,
            CalculoSugestaoPreco calculo, Usuario responsavel, java.time.LocalDateTime criadaEm) {
        return new SugestaoPrecoResponse(id, ordemId, clienteId, execucao == null ? null : execucao.getId(),
                calculo.precoBase(), calculo.ajustePid(), calculo.precoAjustado(), calculo.descontoFidelidade(),
                calculo.precoAposFidelidade(), calculo.pisoMinimo(), calculo.precoSugerido(),
                calculo.ordensConcluidas(), calculo.ordensCanceladas(), calculo.elegivelFidelidade(),
                calculo.justificativa(), responsavel.getId(), responsavel.getNome(), criadaEm);
    }
}
