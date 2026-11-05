-- Limpieza de datos de prueba creados por scripts de humo ("Smoke Prod").
-- Se desactivan (borrado lógico) para que no aparezcan en catálogo, búsqueda ni
-- en el asistente AURA, preservando referencias existentes (carrito, reseñas).
UPDATE products
SET active = false,
    updated_at = CURRENT_TIMESTAMP
WHERE active = true
  AND name LIKE 'Smoke Prod%';
