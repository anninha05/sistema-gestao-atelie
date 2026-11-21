package com.atelie.sistema.ordemservico;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public record ItemOrdemServico(
        @Column(name = "peca_id", nullable = false) UUID pecaId,
        @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2) BigDecimal valorUnitario,
        @Column(name = "medidas") String medidas,
        @Column(name = "instrucoes_costura") String instrucoesCostura) {

    public ItemOrdemServico(UUID pecaId, BigDecimal valorUnitario) {
        this(pecaId, valorUnitario, null, null);
    }
}
