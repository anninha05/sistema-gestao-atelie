CREATE TABLE ordens_servico (
    id UUID PRIMARY KEY,
    versao BIGINT NOT NULL,
    cliente_id UUID NOT NULL REFERENCES clientes(id),
    tipo_fluxo VARCHAR(50) NOT NULL,
    status_atual VARCHAR(50) NOT NULL,
    valor_total NUMERIC(10,2) NOT NULL CHECK (valor_total >= 0),
    desconto NUMERIC(10,2) NOT NULL CHECK (desconto >= 0),
    data_entrega TIMESTAMP,
    inicio_locacao TIMESTAMP,
    fim_locacao TIMESTAMP,
    data_prova TIMESTAMP,
    data_evento TIMESTAMP,
    instrucoes_costura TEXT,
    medidas TEXT,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    CHECK (tipo_fluxo <> 'LOCACAO' OR
           (inicio_locacao IS NOT NULL AND fim_locacao IS NOT NULL AND inicio_locacao < fim_locacao))
);

CREATE TABLE ordens_servico_pecas (
    ordem_servico_id UUID NOT NULL REFERENCES ordens_servico(id),
    peca_id UUID NOT NULL REFERENCES pecas(id),
    valor_unitario NUMERIC(10,2) NOT NULL CHECK (valor_unitario >= 0),
    PRIMARY KEY (ordem_servico_id, peca_id)
);

CREATE INDEX idx_ordens_servico_cliente ON ordens_servico(cliente_id);
CREATE INDEX idx_ordens_servico_pecas_peca ON ordens_servico_pecas(peca_id);

