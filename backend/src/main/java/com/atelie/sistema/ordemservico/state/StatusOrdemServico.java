package com.atelie.sistema.ordemservico.state;

public enum StatusOrdemServico {
    PENDENTE("pendente"),
    RESERVADA("reservada"),
    CONFECCAO("em confecção"),
    PROVA("em prova"),
    EM_PROVA("em prova"),
    AJUSTES("em ajustes"),
    CONCLUIDO("concluída"),
    RETIRADA("retirada"),
    ENTREGUE("entregue"),
    PASSADORIA("em passadoria"),
    CANCELADA("cancelada"),
    DEVOLVIDA("devolvida");

    private final String descricao;

    StatusOrdemServico(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
