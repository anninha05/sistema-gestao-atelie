package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.ordemservico.OrdemServico;
import java.util.ArrayList;
import java.util.List;

public abstract class EstadoEmAndamento extends EstadoOS {

    @Override
    protected final List<AcaoDisponivel> acoesDaEtapa(OrdemServico os) {
        var acoes = new ArrayList<>(acoesEspecificas(os));
        acoes.add(AcaoDisponivel.permitida(AcaoOrdemServico.REAGENDAR));
        acoes.add(AcaoDisponivel.permitida(AcaoOrdemServico.CANCELAR));
        return acoes;
    }

    protected abstract List<AcaoDisponivel> acoesEspecificas(OrdemServico os);

    @Override
    public EstadoOS cancelar(OrdemServico os) {
        return new Cancelada();
    }

    @Override
    public void validarReagendamento() {
    }

    @Override
    public boolean estaEmProducao() {
        return true;
    }
}
