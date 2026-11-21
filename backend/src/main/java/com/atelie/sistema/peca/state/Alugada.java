package com.atelie.sistema.peca.state;

import com.atelie.sistema.peca.Peca;
import com.atelie.sistema.shared.exception.RegraNegocioException;

public class Alugada implements EstadoPeca {

    @Override
    public EstadoPeca alugar(Peca peca) {
        throw new RegraNegocioException("A peça já está alugada.");
    }

    @Override
    public EstadoPeca enviarParaCostura(Peca peca) {
        throw new RegraNegocioException("A peça está alugada e não pode ir para costura antes da devolução.");
    }

    @Override
    public EstadoPeca enviarParaLavagem(Peca peca) {
        return new EmLavagem();
    }

    @Override
    public EstadoPeca devolverParaEstoque(Peca peca) {
        throw new RegraNegocioException("Registre a devolução na ordem de serviço. A peça deve passar pela lavagem antes da liberação.");
    }

    @Override
    public StatusDisponibilidade getStatus() {
        return StatusDisponibilidade.ALUGADA;
    }
}
