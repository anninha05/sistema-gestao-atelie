package com.atelie.sistema.peca.state;

import com.atelie.sistema.peca.Peca;
import com.atelie.sistema.shared.exception.RegraNegocioException;

public class EmLavagem implements EstadoPeca {

    @Override
    public EstadoPeca alugar(Peca peca) {
        throw new RegraNegocioException("A peça está em lavagem e não pode ser alugada.");
    }

    @Override
    public EstadoPeca enviarParaCostura(Peca peca) {
        return new EmCostura();
    }

    @Override
    public EstadoPeca enviarParaLavagem(Peca peca) {
        throw new RegraNegocioException("A peça já está em lavagem.");
    }

    @Override
    public EstadoPeca devolverParaEstoque(Peca peca) {
        return new Disponivel();
    }

    @Override
    public StatusDisponibilidade getStatus() {
        return StatusDisponibilidade.EM_LAVAGEM;
    }
}
