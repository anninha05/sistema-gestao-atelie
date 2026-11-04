package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.shared.exception.RegraNegocioException;

public class EmProva implements EstadoOS {

    @Override
    public void iniciarConfeccao(OrdemServico os) {
        os.setEstado(new EtapaProducao(StatusOrdemServico.AJUSTES));
    }

    @Override
    public void enviarParaProva(OrdemServico os) {
        throw new RegraNegocioException("A O.S. já está em prova.");
    }

    @Override
    public void concluir(OrdemServico os) {
        throw new RegraNegocioException("Finalize os ajustes e a passadoria antes de liberar a ordem.");
    }

    @Override
    public void retirar(OrdemServico os) {
        throw new RegraNegocioException("Finalize os ajustes e a passadoria antes de liberar a ordem.");
    }

    @Override
    public void devolver(OrdemServico os) {
        throw new RegraNegocioException("A roupa ainda não foi retirada para ser devolvida.");
    }

    @Override
    public StatusOrdemServico getStatus() {
        return StatusOrdemServico.EM_PROVA;
    }
}
