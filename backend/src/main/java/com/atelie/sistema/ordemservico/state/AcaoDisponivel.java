package com.atelie.sistema.ordemservico.state;

public record AcaoDisponivel(AcaoOrdemServico acao, String rotulo, String motivoBloqueio) {

    public static AcaoDisponivel permitida(AcaoOrdemServico acao) {
        return new AcaoDisponivel(acao, acao.getRotulo(), null);
    }

    public static AcaoDisponivel permitida(AcaoOrdemServico acao, String rotulo) {
        return new AcaoDisponivel(acao, rotulo, null);
    }

    public static AcaoDisponivel bloqueada(AcaoOrdemServico acao, String rotulo, String motivo) {
        return new AcaoDisponivel(acao, rotulo, motivo);
    }

    public boolean habilitada() {
        return motivoBloqueio == null;
    }
}
