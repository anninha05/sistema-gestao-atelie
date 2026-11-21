package com.atelie.sistema.peca.state;

import com.atelie.sistema.peca.Peca;

public interface EstadoPeca {

    static EstadoPeca de(StatusDisponibilidade status) {
        if (status == null) {
            return new Disponivel();
        }
        return switch (status) {
            case DISPONIVEL -> new Disponivel();
            case ALUGADA -> new Alugada();
            case EM_COSTURA, MANUTENCAO -> new EmCostura();
            case EM_LAVAGEM -> new EmLavagem();
        };
    }

    EstadoPeca alugar(Peca peca);

    EstadoPeca enviarParaCostura(Peca peca);

    EstadoPeca enviarParaLavagem(Peca peca);

    EstadoPeca devolverParaEstoque(Peca peca);

    StatusDisponibilidade getStatus();
}
