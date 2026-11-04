package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.shared.exception.RegraNegocioException;

public class Reservada implements EstadoOS {

    @Override
    public void iniciarConfeccao(OrdemServico os) {
        var etapa = os.getTipoFluxo() == com.atelie.sistema.peca.TipoFluxo.SOB_MEDIDA
                ? StatusOrdemServico.CONFECCAO : StatusOrdemServico.AJUSTES;
        os.setEstado(new EtapaProducao(etapa));
    }

    @Override
    public void enviarParaProva(OrdemServico os) {
        os.setEstado(new EmProva());
    }

    @Override
    public void concluir(OrdemServico os) {
        throw new RegraNegocioException("A O.S. está apenas reservada e precisa ser provada antes de concluída.");
    }

    @Override
    public void retirar(OrdemServico os) {
        throw new RegraNegocioException("A roupa não pode ser retirada enquanto estiver apenas reservada.");
    }

    @Override
    public void devolver(OrdemServico os) {
        throw new RegraNegocioException("A roupa ainda não foi retirada para poder ser devolvida.");
    }

    @Override
    public StatusOrdemServico getStatus() {
        return StatusOrdemServico.RESERVADA;
    }
}
