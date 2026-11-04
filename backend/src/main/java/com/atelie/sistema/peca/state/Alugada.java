package com.atelie.sistema.peca.state;

import com.atelie.sistema.peca.Peca;
import com.atelie.sistema.shared.exception.RegraNegocioException;

public class Alugada implements EstadoPeca {

    @Override
    public void alugar(Peca peca) {
        throw new RegraNegocioException("A peça já está alugada.");
    }

    @Override
    public void enviarParaCostura(Peca peca) {
        throw new RegraNegocioException("A peça está alugada e não pode ir para costura antes da devolução.");
    }

    @Override
    public void enviarParaLavagem(Peca peca) {
        peca.setEstado(new EmLavagem());
    }

    @Override
    public void devolverParaEstoque(Peca peca) {
        peca.setEstado(new Disponivel());
    }

    @Override
    public StatusDisponibilidade getStatus() {
        return StatusDisponibilidade.ALUGADA;
    }
}
