CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE clientes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(255) NOT NULL,
    cpf VARCHAR(14) UNIQUE NOT NULL,
    telefone VARCHAR(20),
    email VARCHAR(100)
);

CREATE TABLE pecas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tipo_peca VARCHAR(50) NOT NULL,      
    estado_atual VARCHAR(50) NOT NULL,   
    descricao VARCHAR(255) NOT NULL,
    tamanho VARCHAR(10),                 
    cor VARCHAR(50),
    valor_base NUMERIC(10, 2) NOT NULL   
);

CREATE TABLE ordens_servico (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cliente_id UUID NOT NULL REFERENCES clientes(id),
    data_abertura TIMESTAMP NOT NULL,
    data_inicio_agendamento TIMESTAMP,   
    data_fim_agendamento TIMESTAMP,      
    valor_total NUMERIC(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE ordens_servico_pecas (
    ordem_servico_id UUID NOT NULL REFERENCES ordens_servico(id),
    peca_id UUID NOT NULL REFERENCES pecas(id),
    PRIMARY KEY (ordem_servico_id, peca_id)
);