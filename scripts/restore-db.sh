#!/usr/bin/env bash
# Restaura un backup generado por backup-db.sh.
# Uso:   ./scripts/restore-db.sh backups/joyeria-YYYYmmdd-HHMMSS.sql.gz
# ATENCIÓN: sobrescribe los datos actuales de la base de datos.
set -euo pipefail

FILE="${1:?uso: ./scripts/restore-db.sh <archivo.sql.gz>}"
DB_USER="${DATABASE_USERNAME:-joyeria_user}"
DB_NAME="${DATABASE_NAME:-joyeria}"

gunzip -c "$FILE" | docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME"
echo "Restauración completada desde $FILE"
