-- Búsqueda eficiente (B-M3): índices trigram sobre los campos consultados con LIKE '%q%'.
-- Se indexa lower(col) para coincidir con LOWER(col) LIKE LOWER(...) en JPQL.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_products_name_trgm
    ON products USING GIN (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_products_description_trgm
    ON products USING GIN (lower(description) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_products_sku_trgm
    ON products USING GIN (lower(sku) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_products_color_trgm
    ON products USING GIN (lower(color) gin_trgm_ops);
