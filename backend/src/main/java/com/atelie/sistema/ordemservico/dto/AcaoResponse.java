package com.atelie.sistema.ordemservico.dto;

import com.atelie.sistema.ordemservico.state.AcaoDisponivel;
import com.atelie.sistema.ordemservico.state.AcaoOrdemServico;
import com.atelie.sistema.usuario.Perfil;
import java.util.List;

public record AcaoResponse(AcaoOrdemServico acao, String rotulo, boolean habilitada, String motivoBloqueio) {

    public static AcaoResponse de(AcaoDisponivel disponivel) {
        return new AcaoResponse(disponivel.acao(), disponivel.rotulo(), disponivel.habilitada(),
                disponivel.motivoBloqueio());
    }

    static List<AcaoResponse> permitidasPara(List<AcaoResponse> acoes, Perfil perfil) {
        return acoes.stream().filter(acao -> acao.acao().permitidaPara(perfil)).toList();
    }
}
