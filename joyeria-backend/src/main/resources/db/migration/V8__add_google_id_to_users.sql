-- V2: Agregar campo google_id para autenticación con Google
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) UNIQUE;

-- Hacer password opcional para usuarios que solo usan Google OAuth
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
