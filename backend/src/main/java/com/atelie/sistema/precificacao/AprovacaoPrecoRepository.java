package com.atelie.sistema.precificacao;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AprovacaoPrecoRepository extends JpaRepository<AprovacaoPreco, Long> {
    boolean existsBySugestaoId(Long sugestaoId);
}
