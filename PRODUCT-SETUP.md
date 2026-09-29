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

`backend/.env.example` lists every backend key, including optional PostgreSQL, session cookies, billing prices, plan labels, and limits.
Spring Boot reads the process environment; it does not automatically load `.env`. Export these variables through your own VPS process manager or Docker configuration.
No remote environment or deployment was changed.

## Accounts and existing records

Spring Security protects workspace APIs, BCrypt hashes passwords, server-side sessions use HttpOnly cookies, and state-changing browser requests require CSRF tokens. Cookies are forwarded by the same-origin Next.js API proxy. Set `SESSION_COOKIE_SECURE=true` for HTTPS production.
Each account has an isolated workspace. Member-directory entries represent people you coordinate, not invited collaborators with their own access.
The migration preserves earlier demo data under owner `legacy`; it is not exposed to newly registered accounts. Old records are not automatically assigned to the first signup.
Session storage is currently in-process: signing in again is required after backend restarts. Shared sessions across multiple backend replicas, email verification, password recovery, and account invitations are not implemented.

## Suggested commercial model

The initial, editable monthly prices are Free ($0), Studio ($19), and Scale ($59) per workspace. These are launch hypotheses, not validated demand or revenue claims. Charge for managing more people, spaces, and active weekly reservations; the core workflow remains usable on Free.

Paid checkout is disabled until backend Stripe credentials and Price IDs are configured. Create matching monthly recurring prices for Studio and Scale, set `STRIPE_PRICE_STUDIO` and `STRIPE_PRICE_SCALE`, and keep the configured display currency/amounts aligned with those prices. Enable Stripe's customer portal for subscription management.
Send Stripe subscription `created`, `updated`, and `deleted` events to the backend `/api/billing/webhook`. Set its signing secret as `STRIPE_WEBHOOK_SECRET`. Plan access updates from verified events, never from the checkout return URL. Checkout and portal are external navigation only after the user clicks their billing action.
Live payment processing requires your keys and an end-to-end Stripe sandbox check before launch. No real payment has been made or tested here.

## Current scheduling semantics

Availability and reservations repeat weekly, Monday through Sunday. Room opening hours currently apply to every day. All entered times use the workspace timezone; changing the timezone relabels wall-clock schedules, it does not shift existing times. Calendar export creates recurring ICS events. Utilization is reserved room hours divided by configured weekly opening hours, not measured attendance.

## UI sources

Uses locally owned [shadcn/ui](https://ui.shadcn.com/docs/components) components, the official [Magic UI Border Beam](https://magicui.design/docs/components/border-beam), and [Motion layout animations](https://motion.dev/docs/react-layout-animations). The palette and Poppins fonts stay local to this project. Motion supports reduced-motion preferences; route contents do not fade in on every navigation.
