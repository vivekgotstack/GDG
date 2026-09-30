# MeetGrid product setup

MeetGrid combines weekly people availability with bookable spaces. Its core use cases are team reviews, student-club meetings, studio sessions, and shared-office room coordination.

## Local use

Run `powershell -ExecutionPolicy Bypass -File .\Start-MeetGrid.ps1` from this folder.
Open http://localhost:3000 and create an account. New accounts start empty.
Members, availability, rooms, reservations, meeting presets, workspace name, and timezone are editable.
The local profile persists accounts and workspace records in `backend/data`.

The planner is at `/app/planner`; rooms at `/app/rooms`; recurring bookings and calendar export at `/app/bookings`; presets at `/app/presets`; utilization at `/app/insights`; profile at `/app/settings`.
Navigation uses normal Next.js links and a persistent workspace shell. An expired session presents sign-in in place instead of redirect loops.

## Configuration keys

Copy `frontend/.env.example` to `frontend/.env.local` for local frontend overrides.
For Vercel, set `API_BASE_URL` to your backend's public origin; this is a server-only variable.
Brand name, tagline, and contact address use the documented `NEXT_PUBLIC_*` keys and require a frontend rebuild.

`backend/.env.example` lists every backend key, including optional PostgreSQL, session cookies, billing integration, and owner bootstrap. Plan labels, prices, and limits are stored in the database and edited in the admin panel.
Spring Boot reads the process environment; it does not automatically load `.env`. Export these variables through your own VPS process manager or Docker configuration.
No remote environment or deployment was changed.

## Accounts and existing records

Spring Security protects workspace APIs, BCrypt hashes passwords, server-side sessions use HttpOnly cookies, and state-changing browser requests require CSRF tokens. Cookies are forwarded by the same-origin Next.js API proxy. Set `SESSION_COOKIE_SECURE=true` for HTTPS production.
Each account has an isolated workspace. Member-directory entries represent people you coordinate, not invited collaborators with their own access.
The migration preserves earlier demo data under owner `legacy`; it is not exposed to newly registered accounts. Old records are not automatically assigned to the first signup.
Session storage is currently in-process: signing in again is required after backend restarts. Shared sessions across multiple backend replicas, email verification, password recovery, and account invitations are not implemented.

## Suggested commercial model

The three monthly paid plans are Gather ($7: 10 people, 3 rooms, 30 weekly bookings, 5 presets), Studio ($12: 30 people, 10 rooms, 150 bookings, 25 presets), and Collective ($20: 100 people, 30 rooms, 600 bookings, 100 presets). All include availability matching, conflict checks, bulk import, exports, and utilization insights.

New and existing free accounts use a no-card Preview workspace (5 people, 2 rooms, 10 bookings, 3 presets). This is not an automatically renewing trial or a fourth paid tier. No existing records are deleted when limits change. Backend limits protect every create endpoint, including batch imports and presets.

Paid checkout is disabled until backend Stripe credentials and Price IDs are configured. Create matching monthly USD recurring prices for all three tiers, and set `STRIPE_PRICE_STARTER`, `STRIPE_PRICE_STUDIO`, and `STRIPE_PRICE_SCALE`. The last key keeps compatibility with the prior tier ID and now represents Collective. Checkout verifies Stripe amount, currency, and monthly recurrence against the database before creating a payment session. Enable Stripe's customer portal for subscription management.
Send Stripe subscription `created`, `updated`, and `deleted` events to the backend `/api/billing/webhook`. Set its signing secret as `STRIPE_WEBHOOK_SECRET`. Plan access updates from verified events, never from the checkout return URL. Checkout and portal are external navigation only after the user clicks their billing action.
Live payment processing requires your keys and an end-to-end Stripe sandbox check before launch. No real payment has been made or tested here.

## Current scheduling semantics

Availability and reservations repeat weekly, Monday through Sunday. Room opening hours currently apply to every day. All entered times use the workspace timezone; changing the timezone relabels wall-clock schedules, it does not shift existing times. Bookings have editable titles and notes, searchable day/room filters, and conflict-checked rescheduling that preserves the original reservation on failure. Edits affect the full weekly series. Calendar export creates recurring ICS events with titles, room locations, and notes; active filters determine which reservations are exported. Utilization is reserved room hours divided by configured weekly opening hours, not measured attendance.

## UI sources

Uses locally owned [shadcn/ui](https://ui.shadcn.com/docs/components) components, the official [Magic UI Border Beam](https://magicui.design/docs/components/border-beam), and [Motion layout animations](https://motion.dev/docs/react-layout-animations). The palette and Poppins fonts stay local to this project. Motion supports reduced-motion preferences; route contents do not fade in on every navigation.

## Administrator and RBAC

The reserved owner identifier is `vivekni1224@nigam`, exactly as requested. This is an account identifier; mail delivery to it has not been verified. Public signup cannot claim it. Bootstrap never grants administrator privileges solely from a signup address.

For local use, `Start-MeetGrid.ps1` generates a random initial password in the ignored `.runtime/admin-credentials.json` and passes it to the backend process. Sign in at `/login` and open `/app/admin`. Keep that file private. It is not committed or exposed to the frontend. Existing owner passwords are not reset on restart.

On your own server, set `ADMIN_EMAIL` and a secret `ADMIN_BOOTSTRAP_PASSWORD` (16–72 UTF-8 bytes) before the first startup. Remove the bootstrap password from the process environment after account creation if desired. If an existing regular account has the reserved identifier, promotion requires its matching password; it is never silently taken over.

USER accounts can manage only their own workspace. ADMIN accounts can edit global branding, contact details, homepage and public-page copy, policies, pricing and limits, and account profiles/roles/status. The owner cannot be suspended or demoted from the panel. Status and role are checked against the database on each authenticated request, so suspension applies to existing sessions. Admin access does not expose stored password hashes or automatically share someone else's workspace.

Admin pages: `/app/admin`, `/app/admin-content`, `/app/admin-plans`, `/app/admin-users`, `/app/admin-audit`. Content and plan updates detect stale versions. Administrative saves write an audit entry. These controls follow [Spring Security request authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html).

## Public pages and useful tools

Independent routes: `/about`, `/contact`, `/help`, `/how-it-works`, `/use-cases`, `/pricing`, `/legal`, `/privacy`, `/terms`, `/refunds`, `/cookies`, `/security`, `/accessibility`. Navigation uses page links; policy tables of contents and accessibility skip links remain intentional in-page anchors.

The contact page uses 8303165648 and vivekgotstack@gmail.com by default and identifies the StackOrcs association. Override these in the admin panel, or use the `NEXT_PUBLIC_SUPPORT_EMAIL`, `NEXT_PUBLIC_SUPPORT_PHONE`, `NEXT_PUBLIC_COMPANY_NAME`, and `NEXT_PUBLIC_COMPANY_URL` fallback values in frontend/.env.example. Contact composition opens a draft in the user's email application; it does not pretend a message was submitted.

Ctrl/Cmd+K opens the keyboard command menu. `/app/tools` links to `/app/import` (preview names, skip duplicates, import the entire batch atomically) and `/app/export` (workspace JSON or people/rooms/bookings CSV). JSON is an export, not an automatic restore format. CSV cells are protected against spreadsheet formula injection. Imports do not invite people or invent availability.

Public wording should be reviewed for the actual operating business and hosting practices before paid launch; no registration address, jurisdiction, certification, or refund promise has been invented. The policies describe the implemented product. Useful policy guidance: [FTC consumer privacy guidance](https://www.ftc.gov/business-guidance/privacy-security/consumer-privacy).
