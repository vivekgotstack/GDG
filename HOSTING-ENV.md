# Copy-paste hosting configuration

The exact placeholder lists are [backend/.env.example](backend/.env.example) and [frontend/.env.example](frontend/.env.example). Spring defaults are in backend/src/main/resources/application.yaml. Replace REPLACE_ values before launch; they are deliberately nonfunctional, not hidden live keys.

## Backend on Hostinger/VPS

Copy backend/.env.example to backend/.env. Set DB_URL/DB_USERNAME/DB_PASSWORD for PostgreSQL, REDIS_HOST/REDIS_PORT/REDIS_PASSWORD, APP_URL to your exact HTTPS frontend origin, and SESSION_COOKIE_SECURE=true. Native Spring reads exported environment variables, not .env automatically. The optional compose.yaml reads backend/.env and creates PostgreSQL, Redis and the API; no remote host is changed by this repository.

Run `docker compose --env-file backend/.env up -d --build`. PostgreSQL and Redis are private to the Docker network. API is bound to localhost:8080; put your HTTPS reverse proxy in front. Keep secrets out of Git. Health/readiness is /actuator/health/readiness and checks PostgreSQL and Redis. API pool size defaults to 8 and HTTP threads to 60. Redis is limited to 128 MB with no eviction: exhaustion fails requests rather than silently removing security counters. Observe memory and tune for actual load.

For a native backend use BIND_ADDRESS=127.0.0.1 behind the same-machine proxy; containers use 0.0.0.0. For local HTTP only, set SESSION_COOKIE_SECURE=false and APP_URL=http://127.0.0.1:3000. There is no H2 fallback.

## Frontend

API_BASE_URL is the HTTPS backend origin, no /api suffix. PROXY_SHARED_SECRET must be the SAME random value on frontend/backend (at least 32 characters). This authenticates the forwarded client address for per-IP rate limits; it is not an authentication bypass. Vercel's platform IP header is used automatically. On another frontend host, enable TRUST_PROXY_IP_HEADER=true only if your reverse proxy overwrites X-Forwarded-For with the real client IP. Otherwise requests share the upstream IP limit.

NEXT_PUBLIC_* branding values are build-time fallbacks. The saved admin content takes precedence. No database, Brevo, mail-encryption, or Razorpay secret belongs in NEXT_PUBLIC variables.

## Brevo and account email

BREVO_API_KEY is your Brevo transactional API key, not SMTP credentials. Verify BREVO_SENDER_EMAIL (default hello@stackorcs.com) or authenticate its domain in Brevo; set sender name and reply-to as desired. The key alone cannot verify sender ownership.

MAIL_ENCRYPTION_KEY must be a base64 encoding of 32 random bytes: generate with `openssl rand -base64 32`. Keep it stable while mail is queued; changing it makes existing encrypted queue payloads unreadable. To rotate, drain the pending queue first, then replace the key. Queued emails retry up to six attempts with backoff, expire with the underlying link, and purge seven days after expiry. Successful provider acceptance clears the body. Brevo acceptance is not proof of inbox delivery; inspect Brevo transactional logs.

Verification links last 24 hours; reset links 20 minutes. PostgreSQL stores only token hashes outside the encrypted mail payload. Links are single-use; GET does not consume them. Password changes invalidate all app sessions. Account email is mandatory for new password registrations; placeholder mail configuration returns a clear unavailable error instead of creating unverifiable accounts.

## Clerk social sign-in

Put NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY and CLERK_SECRET_KEY on the frontend. Put the same CLERK_SECRET_KEY and the exact CLERK_ISSUER on the backend (your Clerk instance HTTPS origin). APP_URL must exactly match the frontend origin accepted as JWT azp. Enable desired providers, such as Google/GitHub, in Clerk and configure production domain/provider callbacks using Clerk's dashboard. Set Clerk's sign-in page to /oauth and completion redirect to /oauth/complete.

No custom JWT email template is needed: the backend verifies the signed session JWT, issuer, expiry and authorized party, then fetches a verified primary email from Clerk's backend API. Existing password accounts are never automatically merged from an email claim. Sign in with the existing password first and link from Settings. Clerk handles its provider verification; Brevo handles MeetGrid's password-account mail.

## Owner and billing

ADMIN_EMAIL defaults to vivekni1224@nigam exactly as requested. ADMIN_BOOTSTRAP_PASSWORD creates the owner initially (16–72 UTF-8 bytes), but does not rotate an existing account's password. Use Settings → Sign-in & security for rotation. Keep bootstrap empty during legacy import; see database/README.md.

Razorpay uses RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET, RAZORPAY_WEBHOOK_SECRET, and RAZORPAY_PLAN_STARTER/STUDIO/SCALE. Remove the old STRIPE_SECRET_KEY, STRIPE_WEBHOOK_SECRET and STRIPE_PRICE_STARTER/STUDIO/SCALE variables. Create three plans in the same Razorpay mode/account as your keys: monthly interval 1, quantity 1, INR amounts 29900 / 59900 / 99900 paise (Gather / Studio / Collective). Copy their plan_ IDs into the corresponding variables. Checkout validates the provider plan against the displayed price/currency. Customized or imported plans may differ; align them before charging. Existing subscriptions keep their purchased provider-plan snapshot when an admin edits the public plan.

Enable Razorpay Subscriptions in test mode and live mode as applicable. The backend creates subscriptions and the frontend opens Standard Checkout with subscription_id. This integration uses recurring mandates, not one-time Orders. Authorization alone does not grant paid access; the backend waits for a confirmed charged billing cycle. In Plans & billing, users can refresh status, resume an unfinished checkout, cancel renewal at the end of an active cycle, and schedule supported card-plan changes at the next cycle. UPI and other methods may require cancellation followed by a new subscription after the paid period. Razorpay sends customer notifications (customer_notify=true).

RAZORPAY_SUBSCRIPTION_CYCLES defaults to 120 monthly cycles and accepts 1–120. Renewal stops at that authorized limit or cancellation. The limit is shown in checkout. Switching test to live mode requires new live keys AND live plan IDs. Historical database migrations/columns remain for import compatibility; they are not used by the Razorpay integration. Any real previous-provider mandate must be cancelled in that provider's dashboard; changing application code cannot cancel it.

Configure the webhook on the BACKEND's public HTTPS URL: https://YOUR_BACKEND_HOST/api/billing/webhook. Choose a separate random webhook secret and put it in RAZORPAY_WEBHOOK_SECRET. Select subscription.authenticated, subscription.activated, subscription.charged, subscription.pending, subscription.halted, subscription.paused, subscription.resumed, subscription.cancelled, subscription.completed, and subscription.updated when available. The server verifies X-Razorpay-Signature over the raw request bytes, records duplicate payloads once, and fetches current subscription state to handle reordered notifications. Periodic reconciliation also refreshes ongoing subscriptions; provider failure does not extend paid access. Previously confirmed access expires according to its saved paid-through date even if webhooks are unavailable.

No Razorpay frontend environment variable is required: authenticated checkout returns only the public key ID from the backend. The key secret and webhook secret remain backend-only. No live payment was made during implementation. Test in Razorpay test mode before enabling live keys; see docs/RAZORPAY.md. If admin-saved legal content still names the previous provider or portal, update those overrides in the content editor.

## Final external setup

Configure PostgreSQL, Redis, HTTPS routing, provider credentials/sender/provider domains, and backups. Backend placeholders live in application.yaml and env examples; frontend values must be provided at build time. See docs/DISTRIBUTION.md for PWA/Tauri/Store steps. No live mail, OAuth, payment, or store submission has been performed.

References: [Brevo transactional API](https://developers.brevo.com/docs/send-a-transactional-email), [Clerk JWT verification](https://clerk.com/docs/guides/sessions/manual-jwt-verification), [Razorpay Subscriptions](https://razorpay.com/docs/payments/subscriptions/integration-guide/).
