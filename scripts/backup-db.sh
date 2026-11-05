#!/usr/bin/env bash
# Backup lógico de PostgreSQL de la tienda AURA.
# Uso:   ./scripts/backup-db.sh [directorio_destino]   (por defecto: ./backups)
# Requiere el contenedor `postgres` en ejecución y las variables de .env.
set -euo pipefail

DEST="${1:-backups}"
mkdir -p "$DEST"

DB_USER="${DATABASE_USERNAME:-joyeria_user}"
DB_NAME="${DATABASE_NAME:-joyeria}"
STAMP="$(date +%Y%m%d-%H%M%S)"
FILE="$DEST/joyeria-$STAMP.sql.gz"

docker compose exec -T postgres pg_dump -U "$DB_USER" -d "$DB_NAME" --clean --if-exists | gzip > "$FILE"
echo "Backup creado: $FILE"

# Retención: elimina backups con más de 30 días
find "$DEST" -name 'joyeria-*.sql.gz' -mtime +30 -delete
