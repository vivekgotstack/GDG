#!/usr/bin/env sh
set -eu
umask 077
cd "$(dirname "$0")/.."
mkdir -p .runtime/backups
backup=".runtime/backups/meetgrid-$(date -u +%Y%m%dT%H%M%SZ).sql"
docker compose --env-file backend/.env exec -T postgres pg_dump --no-owner --no-acl -U meetgrid -d meetgrid > "$backup.partial"
mv "$backup.partial" "$backup"
printf 'PostgreSQL backup saved: %s\n' "$backup"
