# Copy-paste hosting configuration

The exact placeholder lists are [backend/.env.example](backend/.env.example) and [frontend/.env.example](frontend/.env.example). Spring defaults are in backend/src/main/resources/application.yaml. Replace REPLACE_ values before launch; they are deliberately nonfunctional, not hidden live keys.

## Backend on Hostinger/VPS

Copy backend/.env.example to backend/.env. Set DB_URL/DB_USERNAME/DB_PASSWORD for PostgreSQL, REDIS_HOST/REDIS_PORT/REDIS_PASSWORD, APP_URL to your exact HTTPS frontend origin, and SESSION_COOKIE_SECURE=true. Native Spring reads exported environment variables, not .env automatically. The optional compose.yaml reads backend/.env and creates PostgreSQL, Redis and the API; no remote host is changed by this repository.

Run `docker compose --env-file backend/.env up -d --build`. PostgreSQL and Redis are private to the Docker network. API is bound to localhost:8080; put your HTTPS reverse proxy in front. Keep secrets out of Git. Health/readiness is /actuator/health/readiness and checks PostgreSQL and Redis. API pool size defaults to 8 and HTTP threads to 60. Redis is limited to 128 MB with no eviction: exhaustion fails requests rather than silently removing security counters. Observe memory and tune for actual load.

For a native backend use BIND_ADDRESS=127.0.0.1 behind the same-machine proxy; containers use 0.0.0.0. For local HTTP only, set SESSION_COOKIE_SECURE=false and APP_URL=http://127.0.0.1:3000. There is no H2 fallback.

## Frontend

API_BASE_URL is the HTTPS backend origin, no /api suffix. PROXY_SHARED_SECRET must be the SAME random value on frontend/backend (at least 32 characters). This authenticates the forwarded client address for per-IP rate limits; it is not an authentication bypass. Vercel's platform IP header is used automatically. On another frontend host, enable TRUST_PROXY_IP_HEADER=true only if your reverse proxy overwrites X-Forwarded-For with the real client IP. Otherwise requests share the upstream IP limit.

NEXT_PUBLIC_* branding values are build-time fallbacks. The saved admin content takes precedence. No database, Brevo, mail-encryption, or Stripe secret belongs in NEXT_PUBLIC variables.

## Brevo and account email

BREVO_API_KEY is your Brevo transactional API key, not SMTP credentials. Verify BREVO_SENDER_EMAIL (default hello@stackorcs.com) or authenticate its domain in Brevo; set sender name and reply-to as desired. The key alone cannot verify sender ownership.

MAIL_ENCRYPTION_KEY must be a base64 encoding of 32 random bytes: generate with `openssl rand -base64 32`. Keep it stable while mail is queued; changing it makes existing encrypted queue payloads unreadable. To rotate, drain the pending queue first, then replace the key. Queued emails retry up to six attempts with backoff, expire with the underlying link, and purge seven days after expiry. Successful provider acceptance clears the body. Brevo acceptance is not proof of inbox delivery; inspect Brevo transactional logs.

Verification links last 24 hours; reset links 20 minutes. PostgreSQL stores only token hashes outside the encrypted mail payload. Links are single-use; GET does not consume them. Password changes invalidate all app sessions. Account email is mandatory for new password registrations; placeholder mail configuration returns a clear unavailable error instead of creating unverifiable accounts.

## Clerk social sign-in

Put NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY and CLERK_SECRET_KEY on the frontend. Put the same CLERK_SECRET_KEY and the exact CLERK_ISSUER on the backend (your Clerk instance HTTPS origin). APP_URL must exactly match the frontend origin accepted as JWT azp. Enable desired providers, such as Google/GitHub, in Clerk and configure production domain/provider callbacks using Clerk's dashboard. Set Clerk's sign-in page to /oauth and completion redirect to /oauth/complete.

No custom JWT email template is needed: the backend verifies the signed session JWT, issuer, expiry and authorized party, then fetches a verified primary email from Clerk's backend API. Existing password accounts are never automatically merged from an email claim. Sign in with the existing password first and link from Settings. Clerk handles its provider verification; Brevo handles MeetGrid's password-account mail.

## Owner and billing

ADMIN_EMAIL defaults to vivekni1224@nigam exactly as requested. ADMIN_BOOTSTRAP_PASSWORD creates the owner initially (16–72 UTF-8 bytes), but does not rotate an existing account's password. Use Settings → Sign-in & security for rotation. Keep bootstrap empty during legacy import; see database/README.md.

Stripe needs STRIPE_SECRET_KEY, STRIPE_WEBHOOK_SECRET and the three price IDs. New India prices: Gather ₹299, Studio ₹599, Collective ₹999, monthly per workspace. STRIPE_PRICE_STARTER/STUDIO/SCALE must reference matching recurring monthly INR prices. Customized or imported plans may have different currency/amounts; align them first in /app/admin-plans. Configure the customer portal and /api/billing/webhook for customer.subscription.created, updated and deleted. Live billing and provider account eligibility still need your sandbox verification.

## Final external setup

Configure PostgreSQL, Redis, HTTPS routing, provider credentials/sender/provider domains, and backups. Backend placeholders live in application.yaml and env examples; frontend values must be provided at build time. See docs/DISTRIBUTION.md for PWA/Tauri/Store steps. No live mail, OAuth, payment, or store submission has been performed.

References: [Brevo transactional API](https://developers.brevo.com/docs/send-a-transactional-email), [Clerk JWT verification](https://clerk.com/docs/guides/sessions/manual-jwt-verification).
