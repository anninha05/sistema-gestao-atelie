package com.atelie.sistema.ordemservico;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public record ItemOrdemServico(
        @Column(name = "peca_id", nullable = false) UUID pecaId,
        @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2) BigDecimal valorUnitario) {
}
