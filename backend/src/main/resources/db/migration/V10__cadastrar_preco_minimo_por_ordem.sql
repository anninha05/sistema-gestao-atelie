CREATE TABLE precificacao_pisos_ordem (
    ordem_servico_id UUID PRIMARY KEY REFERENCES ordens_servico(id),
    valor NUMERIC(10, 2) NOT NULL CHECK (valor > 0),
    responsavel_id BIGINT NOT NULL REFERENCES usuarios(id),
    atualizado_em TIMESTAMP NOT NULL
);
