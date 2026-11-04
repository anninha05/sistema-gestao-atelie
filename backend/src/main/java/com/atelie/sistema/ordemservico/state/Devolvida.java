package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.shared.exception.RegraNegocioException;

public class Devolvida implements EstadoOS {

    @Override
    public void iniciarConfeccao(OrdemServico os) {
        throw new RegraNegocioException("A O.S. já foi finalizada com devolução.");
    }

    @Override
    public void enviarParaProva(OrdemServico os) {
        throw new RegraNegocioException("A O.S. já foi finalizada com devolução.");
    }

    @Override
    public void concluir(OrdemServico os) {
        throw new RegraNegocioException("A O.S. já foi finalizada.");
    }

    @Override
    public void retirar(OrdemServico os) {
        throw new RegraNegocioException("A O.S. já foi devolvida e encerrada.");
    }

    @Override
    public void devolver(OrdemServico os) {
        throw new RegraNegocioException("A O.S. já foi devolvida anteriormente.");
    }

    @Override
    public StatusOrdemServico getStatus() {
        return StatusOrdemServico.DEVOLVIDA;
    }
}
