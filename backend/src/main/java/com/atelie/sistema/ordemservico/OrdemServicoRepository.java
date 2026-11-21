package com.atelie.sistema.ordemservico;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select os from OrdemServico os where os.id = :id")
    Optional<OrdemServico> buscarParaAtualizacao(UUID id);

    @Query("""
            select count(i) from OrdemServico os join os.itens i
            where i.pecaId = :peca and os.inicioLocacao < :fim and os.fimLocacao > :inicio
              and os.statusAtual not in (com.atelie.sistema.ordemservico.state.StatusOrdemServico.DEVOLVIDA,
                                         com.atelie.sistema.ordemservico.state.StatusOrdemServico.CANCELADA)
            """)
    long contarReservas(UUID peca, LocalDateTime inicio, LocalDateTime fim);

    @Query("""
            select count(i) from OrdemServico os join os.itens i
            where i.pecaId = :peca and os.id <> :ordemId
              and os.inicioLocacao < :fim and os.fimLocacao > :inicio
              and os.statusAtual not in (com.atelie.sistema.ordemservico.state.StatusOrdemServico.DEVOLVIDA,
                                         com.atelie.sistema.ordemservico.state.StatusOrdemServico.CANCELADA)
            """)
    long contarReservasDeOutrasOrdens(UUID peca, LocalDateTime inicio, LocalDateTime fim, UUID ordemId);

    List<OrdemServico> findByClienteId(UUID clienteId);
}
