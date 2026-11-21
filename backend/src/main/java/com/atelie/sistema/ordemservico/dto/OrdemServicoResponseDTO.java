package com.atelie.sistema.ordemservico.dto;

import com.atelie.sistema.ordemservico.ItemOrdemServico;
import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import com.atelie.sistema.peca.TipoFluxo;
import com.atelie.sistema.usuario.Perfil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrdemServicoResponseDTO(
        UUID id,
        UUID clienteId,
        String clienteNome,
        TipoFluxo tipoFluxo,
        StatusOrdemServico statusAtual,
        BigDecimal valorTotal,
        LocalDateTime dataEntrega,
        LocalDateTime criadoEm,
        List<ItemOrdemServico> itens,
        BigDecimal desconto,
        LocalDateTime inicioLocacao,
        LocalDateTime fimLocacao,
        LocalDateTime dataProva,
        LocalDateTime dataEvento,
        String instrucoesCostura,
        String medidas,
        boolean pendenciaAcerto,
        List<AcaoResponse> acoes) {

    public OrdemServicoResponseDTO paraPerfil(Perfil perfil) {
        return new OrdemServicoResponseDTO(id, clienteId, clienteNome, tipoFluxo, statusAtual, valorTotal,
                dataEntrega, criadoEm, itens, desconto, inicioLocacao, fimLocacao, dataProva, dataEvento,
                instrucoesCostura, medidas, pendenciaAcerto, AcaoResponse.permitidasPara(acoes, perfil));
    }
}
