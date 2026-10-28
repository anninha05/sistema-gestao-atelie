package com.atelie.sistema.ordemservico;

import java.util.Optional;
import java.util.List;

public interface OrdemServicoRepositoryGateway {

    OrdemServico salvar(OrdemServico ordemServico);

    Optional<OrdemServico> buscarPorId(Long id);

    List<OrdemServico> buscarPorStatus(String statusAtual);

    List<OrdemServico> buscarPorCliente(Long clienteId);
}