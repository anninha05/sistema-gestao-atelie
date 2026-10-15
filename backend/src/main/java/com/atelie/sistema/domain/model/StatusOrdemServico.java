package com.atelie.sistema.domain.model;

public enum StatusOrdemServico {
    PENDENTE, // O.S. criada, mas o trabalho ainda não começou
    CONFECCAO, // Costureira está trabalhando na peça
    PROVA, // Cliente veio experimentar
    AJUSTES, // Voltou para a costureira após a prova
    CONCLUIDO, // Roupa pronta, aguardando retirada
    ENTREGUE // Cliente levou a roupa
}