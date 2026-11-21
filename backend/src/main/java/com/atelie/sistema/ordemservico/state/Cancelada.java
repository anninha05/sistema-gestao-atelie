package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.shared.exception.RegraNegocioException;

public class Cancelada extends EstadoEncerrado {

    @Override
    public StatusOrdemServico getStatus() {
        return StatusOrdemServico.CANCELADA;
    }

    @Override
    protected boolean aceitaRecebimentos() {
        return false;
    }

    @Override
    public void validarRecebimento() {
        throw new RegraNegocioException(
                "Não é possível registrar pagamentos em uma ordem cancelada; consulte a pendência de acerto.");
    }
}
