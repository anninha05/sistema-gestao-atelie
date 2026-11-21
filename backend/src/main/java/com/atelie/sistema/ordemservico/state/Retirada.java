package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.shared.exception.RegraNegocioException;
import java.util.List;

public class Retirada extends EstadoOS {

    @Override
    public StatusOrdemServico getStatus() {
        return StatusOrdemServico.RETIRADA;
    }

    @Override
    protected List<AcaoDisponivel> acoesDaEtapa(OrdemServico os) {
        return os.getTipoFluxo() == TipoFluxo.LOCACAO
                ? List.of(AcaoDisponivel.permitida(AcaoOrdemServico.DEVOLVER))
                : List.of();
    }

    @Override
    public EstadoOS devolver(OrdemServico os) {
        if (os.getTipoFluxo() != TipoFluxo.LOCACAO) {
            throw new RegraNegocioException("Somente ordens de locação podem ser devolvidas.");
        }
        return new Devolvida();
    }
}
