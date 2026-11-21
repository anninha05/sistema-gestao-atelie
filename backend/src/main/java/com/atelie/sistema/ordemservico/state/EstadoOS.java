package com.atelie.sistema.ordemservico.state;

import static com.atelie.sistema.ordemservico.state.AcaoOrdemServico.*;

import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.util.ArrayList;
import java.util.List;

public abstract class EstadoOS {

    public static EstadoOS de(StatusOrdemServico status) {
        if (status == null) {
            return new Reservada();
        }
        return switch (status) {
            case PENDENTE, RESERVADA -> new Reservada();
            case CONFECCAO -> new EmConfeccao();
            case PROVA, EM_PROVA -> new EmProva();
            case AJUSTES -> new EmAjustes();
            case PASSADORIA -> new EmPassadoria();
            case CONCLUIDO -> new Concluida();
            case RETIRADA -> new Retirada();
            case ENTREGUE -> new Entregue();
            case DEVOLVIDA -> new Devolvida();
            case CANCELADA -> new Cancelada();
        };
    }

    public abstract StatusOrdemServico getStatus();

    public final List<AcaoDisponivel> acoesDisponiveis(OrdemServico os) {
        var acoes = new ArrayList<>(acoesDaEtapa(os));
        if (aceitaRecebimentos() && !os.estaQuitada()) {
            acoes.add(AcaoDisponivel.permitida(REGISTRAR_RECEBIMENTO));
        }
        return List.copyOf(acoes);
    }

    protected abstract List<AcaoDisponivel> acoesDaEtapa(OrdemServico os);

    public EstadoOS iniciarConfeccao(OrdemServico os) {
        throw recusar(INICIAR_CONFECCAO);
    }

    public EstadoOS enviarParaProva(OrdemServico os) {
        throw recusar(ENVIAR_PARA_PROVA);
    }

    public EstadoOS iniciarPassadoria(OrdemServico os) {
        throw recusar(INICIAR_PASSADORIA);
    }

    public EstadoOS concluir(OrdemServico os) {
        throw recusar(CONCLUIR);
    }

    public EstadoOS retirar(OrdemServico os) {
        throw recusar(RETIRAR);
    }

    public EstadoOS devolver(OrdemServico os) {
        throw recusar(DEVOLVER);
    }

    public EstadoOS cancelar(OrdemServico os) {
        throw recusar(CANCELAR);
    }

    public void validarReagendamento() {
        throw recusar(REAGENDAR);
    }

    public void validarRecebimento() {
        if (!aceitaRecebimentos()) {
            throw recusar(REGISTRAR_RECEBIMENTO);
        }
    }

    protected boolean aceitaRecebimentos() {
        return true;
    }

    public boolean estaEmProducao() {
        return false;
    }

    protected final RegraNegocioException recusar(AcaoOrdemServico acao) {
        return new RegraNegocioException(acao.mensagemDeRecusa(getStatus()));
    }
}
