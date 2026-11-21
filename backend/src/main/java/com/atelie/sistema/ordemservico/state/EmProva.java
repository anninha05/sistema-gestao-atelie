package com.atelie.sistema.ordemservico.state;

import static com.atelie.sistema.ordemservico.state.AcaoOrdemServico.*;

import com.atelie.sistema.ordemservico.OrdemServico;
import java.util.List;

public class EmProva extends EstadoEmAndamento {

    @Override
    public StatusOrdemServico getStatus() {
        return StatusOrdemServico.EM_PROVA;
    }

    @Override
    protected List<AcaoDisponivel> acoesEspecificas(OrdemServico os) {
        return List.of(AcaoDisponivel.permitida(INICIAR_CONFECCAO, "Iniciar ajustes"),
                AcaoDisponivel.permitida(INICIAR_PASSADORIA));
    }

    @Override
    public EstadoOS iniciarConfeccao(OrdemServico os) {
        return new EmAjustes();
    }

    @Override
    public EstadoOS iniciarPassadoria(OrdemServico os) {
        return new EmPassadoria();
    }
}
