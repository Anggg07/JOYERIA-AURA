-- Permitir pedidos de invitado (sin usuario registrado)
ALTER TABLE orders ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS guest_email VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS guest_token VARCHAR(64);
CREATE UNIQUE INDEX IF NOT EXISTS ux_orders_guest_token ON orders(guest_token) WHERE guest_token IS NOT NULL;