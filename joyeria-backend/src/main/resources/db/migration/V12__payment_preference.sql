-- Fase 5: integración de pagos con pasarela (Mercado Pago / sandbox).
-- Almacena el id de la preferencia de pago y añade índices de búsqueda.
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS preference_id VARCHAR(120);

CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_preference_id
    ON payments (preference_id)
    WHERE preference_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payments_provider_reference
    ON payments (provider_reference);

CREATE INDEX IF NOT EXISTS idx_payments_order_id
    ON payments (order_id);
