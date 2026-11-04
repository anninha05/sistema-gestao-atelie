package com.atelie.sistema.ordemservico.state;

import com.atelie.sistema.ordemservico.OrdemServico;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.shared.exception.RegraNegocioException;

import static com.atelie.sistema.ordemservico.state.StatusOrdemServico.*;

public record EtapaProducao(StatusOrdemServico getStatus) implements EstadoOS {

    @Override
    public void iniciarConfeccao(OrdemServico os) {
        throw new RegraNegocioException("A ordem já entrou em produção.");
    }

    @Override
    public void enviarParaProva(OrdemServico os) {
        if (getStatus != CONFECCAO && getStatus != AJUSTES) {
            throw new RegraNegocioException("A ordem precisa estar em confecção ou ajustes para ir à prova.");
        }
        os.setEstado(new EmProva());
    }

    @Override
    public void concluir(OrdemServico os) {
        if (getStatus != PASSADORIA) {
            throw new RegraNegocioException("Finalize a passadoria antes de liberar a ordem.");
        }
        os.setEstado(new EtapaProducao(CONCLUIDO));
    }

    @Override
    public void retirar(OrdemServico os) {
        if (getStatus != CONCLUIDO) {
            throw new RegraNegocioException("A ordem ainda não está pronta para retirada ou entrega.");
        }
        os.setEstado(os.getTipoFluxo() == TipoFluxo.LOCACAO
                ? new Retirada() : new EtapaProducao(ENTREGUE));
    }

    @Override
    public void devolver(OrdemServico os) {
        throw new RegraNegocioException("Somente locações retiradas podem ser devolvidas.");
    }
}
