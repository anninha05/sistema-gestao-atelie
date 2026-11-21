package com.atelie.sistema.ordemservico.state;

import static com.atelie.sistema.ordemservico.state.AcaoOrdemServico.*;

import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.peca.TipoFluxo;
import java.util.List;

public class Reservada extends EstadoEmAndamento {

    @Override
    public StatusOrdemServico getStatus() {
        return StatusOrdemServico.RESERVADA;
    }

    @Override
    protected List<AcaoDisponivel> acoesEspecificas(OrdemServico os) {
        String inicio = os.getTipoFluxo() == TipoFluxo.SOB_MEDIDA ? "Iniciar confecção" : "Iniciar ajustes";
        return List.of(AcaoDisponivel.permitida(INICIAR_CONFECCAO, inicio),
                AcaoDisponivel.permitida(ENVIAR_PARA_PROVA));
    }

    @Override
    public EstadoOS iniciarConfeccao(OrdemServico os) {
        return os.getTipoFluxo() == TipoFluxo.SOB_MEDIDA ? new EmConfeccao() : new EmAjustes();
    }

    @Override
    public EstadoOS enviarParaProva(OrdemServico os) {
        return new EmProva();
    }
}
