package com.atelie.sistema.ordemservico.state;

public class Entregue extends EstadoEncerrado {

    @Override
    public StatusOrdemServico getStatus() {
        return StatusOrdemServico.ENTREGUE;
    }
}
