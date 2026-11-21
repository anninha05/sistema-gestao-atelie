package com.atelie.sistema.precificacao;

import com.atelie.sistema.shared.exception.RegraNegocioException;
import com.atelie.sistema.usuario.Usuario;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "precificacao_pisos_ordem")
public class PisoPrecoOrdem {
    @Id
    private UUID ordemServicoId;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;
    @Column(nullable = false)
    private LocalDateTime atualizadoEm;

    protected PisoPrecoOrdem() {}
    public PisoPrecoOrdem(UUID ordemId, BigDecimal valor, Usuario responsavel) {
        if (ordemId == null || responsavel == null || valor == null || valor.signum() <= 0
                || valor.stripTrailingZeros().scale() > 2 || valor.precision() - valor.scale() > 8)
            throw new RegraNegocioException("Informe um preço mínimo positivo, com até duas casas decimais e oito dígitos inteiros.");
        this.ordemServicoId = ordemId;
        this.valor = valor.setScale(2);
        this.responsavel = responsavel;
        this.atualizadoEm = LocalDateTime.now();
    }
    public BigDecimal getValor() { return valor; }
}
