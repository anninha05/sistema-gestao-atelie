package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.ordemservico.OrdemServico;
import java.util.List;

public abstract class EstadoEncerrado extends EstadoOS {

    @Override
    protected final List<AcaoDisponivel> acoesDaEtapa(OrdemServico os) {
        return List.of();
    }
}
