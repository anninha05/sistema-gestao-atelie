package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.ordemservico.OrdemServico;
import java.util.List;

public class EmConfeccao extends EstadoEmAndamento {

    @Override
    public StatusOrdemServico getStatus() {
        return StatusOrdemServico.CONFECCAO;
    }

    @Override
    protected List<AcaoDisponivel> acoesEspecificas(OrdemServico os) {
        return List.of(AcaoDisponivel.permitida(AcaoOrdemServico.ENVIAR_PARA_PROVA));
    }

    @Override
    public EstadoOS enviarParaProva(OrdemServico os) {
        return new EmProva();
    }
}
