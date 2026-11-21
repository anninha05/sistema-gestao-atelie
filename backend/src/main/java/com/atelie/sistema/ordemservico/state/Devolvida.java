package com.atelie.sistema.ordemservico.state;

public class Devolvida extends EstadoEncerrado {

    @Override
    public StatusOrdemServico getStatus() {
        return StatusOrdemServico.DEVOLVIDA;
    }
}
