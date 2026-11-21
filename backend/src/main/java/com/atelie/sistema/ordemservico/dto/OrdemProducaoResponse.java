package com.atelie.sistema.ordemservico.dto;

import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.usuario.Perfil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrdemProducaoResponse(UUID id, String clienteNome, TipoFluxo tipoFluxo,
        StatusOrdemServico statusAtual, LocalDateTime dataEntrega, LocalDateTime inicioLocacao,
        LocalDateTime dataProva, LocalDateTime dataEvento, String medidas, String instrucoesCostura,
        List<Item> pecas, List<AcaoResponse> acoes) {

    public record Item(UUID pecaId, String medidas, String instrucoesCostura) {}

    public OrdemProducaoResponse paraPerfil(Perfil perfil) {
        return new OrdemProducaoResponse(id, clienteNome, tipoFluxo, statusAtual, dataEntrega, inicioLocacao,
                dataProva, dataEvento, medidas, instrucoesCostura, pecas, AcaoResponse.permitidasPara(acoes, perfil));
    }
}
