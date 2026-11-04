package com.atelie.sistema.peca.state;

import com.atelie.sistema.peca.Peca;
import com.atelie.sistema.shared.exception.RegraNegocioException;

public class EmLavagem implements EstadoPeca {

    @Override
    public void alugar(Peca peca) {
        throw new RegraNegocioException("A peça está em lavagem e não pode ser alugada.");
    }

    @Override
    public void enviarParaCostura(Peca peca) {
        peca.setEstado(new EmCostura());
    }

    @Override
    public void enviarParaLavagem(Peca peca) {
        throw new RegraNegocioException("A peça já está em lavagem.");
    }

    @Override
    public void devolverParaEstoque(Peca peca) {
        peca.setEstado(new Disponivel());
    }

    @Override
    public StatusDisponibilidade getStatus() {
        return StatusDisponibilidade.EM_LAVAGEM;
    }
}
