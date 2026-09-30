# PostgreSQL storage and recovery

PostgreSQL is the only application database. Flyway applies the SQL files in `backend/src/main/resources/db/migration` automatically. Never manually rerun applied migrations.

The old local workspace was exported to `.runtime/postgres-import.sql` (39 records). It contains private account hashes and is excluded from Git. Copy it directly to your VPS. The old source file is retained only as an offline recovery backup; the app does not open it.

For a NEW empty database, first start the API with `ADMIN_BOOTSTRAP_PASSWORD=` empty so Flyway creates the schema without a conflicting new owner. Keep the API private while importing. Then run once:

```sh
docker compose --env-file backend/.env exec -T postgres psql -v ON_ERROR_STOP=1 -U meetgrid -d meetgrid < .runtime/postgres-import.sql
```

The import is transactional. Conflicts abort instead of silently replacing accounts. Existing passwords and customized plans/content are preserved, including historical USD prices; review Plans & limits after import if you want the new India launch prices. Imported ordinary accounts must verify their email. Admins remain verified. The bootstrap password does not reset an existing owner password; use Settings → Sign-in & security.

Application records, hashed single-use tokens, and encrypted queued emails live in PostgreSQL. Redis contains temporary sessions and counters; restarting Redis signs users out. Clerk stores OAuth identities and Brevo processes emails, as required by those integrations.

Run `sh scripts/backup-postgres.sh` for a full SQL backup. Restore a full pg_dump into a NEW database using psql with ON_ERROR_STOP; do not run Flyway first because the full dump includes schema history. Keep encrypted off-server backups and verify restores.
