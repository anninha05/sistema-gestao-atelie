package com.atelie.sistema.peca.state;

import com.atelie.sistema.peca.Peca;
import com.atelie.sistema.shared.exception.RegraNegocioException;

public class Disponivel implements EstadoPeca {

    @Override
    public EstadoPeca alugar(Peca peca) {
        return new Alugada();
    }

    @Override
    public EstadoPeca enviarParaCostura(Peca peca) {
        return new EmCostura();
    }

    @Override
    public EstadoPeca enviarParaLavagem(Peca peca) {
        return new EmLavagem();
    }

    @Override
    public EstadoPeca devolverParaEstoque(Peca peca) {
        throw new RegraNegocioException("A peça já está disponível no estoque.");
    }

    @Override
    public StatusDisponibilidade getStatus() {
        return StatusDisponibilidade.DISPONIVEL;
    }
}
