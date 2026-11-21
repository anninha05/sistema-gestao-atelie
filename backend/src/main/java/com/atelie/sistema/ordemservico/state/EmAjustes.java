package com.atelie.sistema.ordemservico.state;

import static com.atelie.sistema.ordemservico.state.AcaoOrdemServico.*;

import com.atelie.sistema.ordemservico.OrdemServico;
import java.util.List;

public class EmAjustes extends EstadoEmAndamento {

    @Override
    public StatusOrdemServico getStatus() {
        return StatusOrdemServico.AJUSTES;
    }

    @Override
    protected List<AcaoDisponivel> acoesEspecificas(OrdemServico os) {
        return List.of(AcaoDisponivel.permitida(ENVIAR_PARA_PROVA), AcaoDisponivel.permitida(INICIAR_PASSADORIA));
    }

    @Override
    public EstadoOS enviarParaProva(OrdemServico os) {
        return new EmProva();
    }

    @Override
    public EstadoOS iniciarPassadoria(OrdemServico os) {
        return new EmPassadoria();
    }
}
