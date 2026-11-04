package com.atelie.sistema.ordemservico;

import com.atelie.sistema.ordemservico.state.StatusOrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, UUID> {

    @org.springframework.data.jpa.repository.Query("""
            select count(i) from OrdemServico os join os.itens i
            where i.pecaId = :peca and os.inicioLocacao < :fim and os.fimLocacao > :inicio
              and os.statusAtual not in (com.atelie.sistema.ordemservico.state.StatusOrdemServico.DEVOLVIDA,
                                         com.atelie.sistema.ordemservico.state.StatusOrdemServico.CANCELADA)
            """)
    long contarReservas(UUID peca, java.time.LocalDateTime inicio, java.time.LocalDateTime fim);

    List<OrdemServico> findByStatusAtual(StatusOrdemServico statusAtual);

    List<OrdemServico> findByClienteId(UUID clienteId);
}
