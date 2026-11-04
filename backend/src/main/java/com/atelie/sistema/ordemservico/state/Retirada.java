package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.shared.exception.RegraNegocioException;

public class Retirada implements EstadoOS {

    @Override
    public void iniciarConfeccao(OrdemServico os) {
        throw new RegraNegocioException("A roupa já foi retirada pelo cliente.");
    }

    @Override
    public void enviarParaProva(OrdemServico os) {
        throw new RegraNegocioException("A roupa já foi retirada pelo cliente.");
    }

    @Override
    public void concluir(OrdemServico os) {
        // Já concluída/retirada
    }

    @Override
    public void retirar(OrdemServico os) {
        throw new RegraNegocioException("A roupa já foi retirada.");
    }

    @Override
    public void devolver(OrdemServico os) {
        os.setEstado(new Devolvida());
    }

    @Override
    public StatusOrdemServico getStatus() {
        return StatusOrdemServico.RETIRADA;
    }
}
