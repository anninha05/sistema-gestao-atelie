ALTER TABLE ordens_servico_pecas
    ADD COLUMN medidas TEXT,
    ADD COLUMN instrucoes_costura TEXT;

UPDATE ordens_servico_pecas osp
SET medidas = os.medidas,
    instrucoes_costura = os.instrucoes_costura
FROM ordens_servico os
WHERE osp.ordem_servico_id = os.id;
