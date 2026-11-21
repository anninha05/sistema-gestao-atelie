package com.atelie.sistema.ordemservico;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    @Query("select coalesce(sum(p.saldoDevedor), 0) from Pagamento p where p.ordemServico.statusAtual <> com.atelie.sistema.ordemservico.state.StatusOrdemServico.CANCELADA")
    BigDecimal somarSaldoEmAberto();
}
