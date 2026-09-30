# MeetGrid product setup

MeetGrid coordinates weekly people availability with shared rooms: team reviews, student clubs, studios and shared offices. Every workspace starts with its own editable directory, room definitions and meeting presets.

## Included

- Independent public, legal, support, pricing and workspace routes with pastel styling, Poppins, shadcn components, and restrained Motion.
- Weekly availability matching, room conflict prevention, editable reservation titles/notes, rescheduling, cancellation, search/filter controls, calendar export and utilization.
- Pagination for people, rooms, reservations, meeting options and presets. These workspace lists are currently fetched within plan bounds and paginated in the client; account administration uses server pagination.
- Bulk member import, CSV/JSON exports and keyboard commands.
- PostgreSQL-only durable storage, versioned Flyway migrations, Redis sessions and atomic rate-limit counters.
- Spring Security password auth, Brevo verification/reset mail with encrypted PostgreSQL retry queue, password rotation/session revocation, optional verified Clerk social identities.
- Admin content/policy/plan/account controls and audit history. Owner identifier: vivekni1224@nigam. No automatic role elevation from public signup.
- India launch prices ₹299/₹599/₹999 per workspace monthly; admin-editable currency/limits and Stripe price validation. Preview remains limited and no-card.
- Production PWA manifest, icons, public offline screen and install controls. Tauri desktop packaging and a manual Windows-build workflow.

## Run/configure

Read HOSTING-ENV.md and copy backend/.env.example / frontend/.env.example. PostgreSQL and Redis must be available; there is no H2 fallback. Start-MeetGrid.ps1 uses Docker Compose. Native execution requires exported environment values. No production host is changed automatically.

Old records are preserved in the private .runtime/postgres-import.sql; see database/README.md for one-time import and backups. This file contains account hashes and is excluded from Git.

Read docs/PRICING.md for launch-price assumptions and break-even math; docs/DISTRIBUTION.md for installation and store requirements.

## Exact product boundaries

Directory members are scheduling records, not invited users with shared-account access. Availability and reservations repeat weekly; opening hours currently apply across all seven days. Changing a workspace timezone relabels existing wall-clock schedules. Utilization measures reserved time, not attendance. Email delivery is for authentication; meeting reminder automation, calendar-provider sync, public booking links, shared-workspace invitations and native system-browser OAuth are not implemented. JSON export is not automatic restore.

Brevo sender/domain verification, Clerk providers/domains, Stripe account configuration, live provider checks, HTTPS, database/Redis provisioning and store submission require your external accounts. Placeholder credentials keep unconfigured integrations unavailable. No production readiness, store approval, or profit guarantee is claimed.
