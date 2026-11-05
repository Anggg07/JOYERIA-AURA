-- B-B6: índices faltantes en claves foráneas y unicidad de reseñas por (usuario, producto)
CREATE INDEX IF NOT EXISTS idx_support_tickets_user_id ON support_tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_ticket_responses_ticket_id ON ticket_responses(ticket_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews(user_id);

-- Evita duplicados previos antes de exigir la unicidad (conserva la reseña más reciente)
DELETE FROM reviews a USING reviews b
WHERE a.id < b.id AND a.user_id = b.user_id AND a.product_id = b.product_id;

CREATE UNIQUE INDEX IF NOT EXISTS ux_reviews_user_product ON reviews(user_id, product_id);
