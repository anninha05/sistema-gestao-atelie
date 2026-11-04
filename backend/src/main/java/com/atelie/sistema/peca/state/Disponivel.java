package com.atelie.sistema.peca.state;

import com.atelie.sistema.peca.Peca;
import com.atelie.sistema.shared.exception.RegraNegocioException;

public class Disponivel implements EstadoPeca {

    @Override
    public void alugar(Peca peca) {
        peca.setEstado(new Alugada());
    }

    @Override
    public void enviarParaCostura(Peca peca) {
        peca.setEstado(new EmCostura());
    }

    @Override
    public void enviarParaLavagem(Peca peca) {
        peca.setEstado(new EmLavagem());
    }

    @Override
    public void devolverParaEstoque(Peca peca) {
        throw new RegraNegocioException("A peça já está disponível no estoque.");
    }

    @Override
    public StatusDisponibilidade getStatus() {
        return StatusDisponibilidade.DISPONIVEL;
    }
}
