package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.util.List;

public class Concluida extends EstadoEmAndamento {

    static final String SALDO_PENDENTE = "Quite o saldo devedor da ordem antes de registrar a retirada ou entrega.";

    @Override
    public StatusOrdemServico getStatus() {
        return StatusOrdemServico.CONCLUIDO;
    }

    @Override
    protected List<AcaoDisponivel> acoesEspecificas(OrdemServico os) {
        String rotulo = os.getTipoFluxo() == TipoFluxo.LOCACAO ? "Registrar retirada" : "Registrar entrega";
        return List.of(os.estaQuitada()
                ? AcaoDisponivel.permitida(AcaoOrdemServico.RETIRAR, rotulo)
                : AcaoDisponivel.bloqueada(AcaoOrdemServico.RETIRAR, rotulo, SALDO_PENDENTE));
    }

    @Override
    public EstadoOS retirar(OrdemServico os) {
        if (!os.estaQuitada()) {
            throw new RegraNegocioException(SALDO_PENDENTE);
        }
        return os.getTipoFluxo() == TipoFluxo.LOCACAO ? new Retirada() : new Entregue();
    }
}
