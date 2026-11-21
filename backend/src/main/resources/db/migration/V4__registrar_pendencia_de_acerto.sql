ALTER TABLE ordens_servico
    ADD COLUMN pendencia_acerto BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE ordens_servico os
SET pendencia_acerto = TRUE
FROM pagamentos p
WHERE p.ordem_servico_id = os.id
  AND os.status_atual = 'CANCELADA'
  AND p.valor_pago > 0;
