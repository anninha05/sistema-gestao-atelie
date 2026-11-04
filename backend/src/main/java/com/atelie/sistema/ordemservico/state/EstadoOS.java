package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.ordemservico.OrdemServico;

public interface EstadoOS {

    void iniciarConfeccao(OrdemServico os);

    void enviarParaProva(OrdemServico os);

    void concluir(OrdemServico os);

    void retirar(OrdemServico os);

    void devolver(OrdemServico os);

    StatusOrdemServico getStatus();
}
