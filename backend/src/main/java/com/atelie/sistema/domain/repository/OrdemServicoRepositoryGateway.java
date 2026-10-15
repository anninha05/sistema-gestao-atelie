package com.atelie.sistema.domain.repository;

import com.atelie.sistema.domain.model.OrdemServico;
import java.util.Optional;
import java.util.List;

public interface OrdemServicoRepositoryGateway {

    OrdemServico salvar(OrdemServico ordemServico);

    Optional<OrdemServico> buscarPorId(Long id);

    List<OrdemServico> buscarPorStatus(String statusAtual);

    List<OrdemServico> buscarPorCliente(Long clienteId);
}