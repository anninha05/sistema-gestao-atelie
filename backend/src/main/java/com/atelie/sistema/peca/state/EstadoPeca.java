package com.atelie.sistema.peca.state;

import com.atelie.sistema.peca.Peca;

public interface EstadoPeca {

    void alugar(Peca peca);

    void enviarParaCostura(Peca peca);

    void enviarParaLavagem(Peca peca);

    void devolverParaEstoque(Peca peca);

    StatusDisponibilidade getStatus();
}
