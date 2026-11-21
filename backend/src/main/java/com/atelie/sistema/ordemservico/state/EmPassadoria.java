package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.ordemservico.OrdemServico;
import java.util.List;

public class EmPassadoria extends EstadoEmAndamento {

    @Override
    public StatusOrdemServico getStatus() {
        return StatusOrdemServico.PASSADORIA;
    }

    @Override
    protected List<AcaoDisponivel> acoesEspecificas(OrdemServico os) {
        return List.of(AcaoDisponivel.permitida(AcaoOrdemServico.CONCLUIR));
    }

    @Override
    public EstadoOS concluir(OrdemServico os) {
        return new Concluida();
    }
}
