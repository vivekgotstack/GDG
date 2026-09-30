# Hosting environment values

Replace every `REPLACE_...` placeholder. These are configuration templates, not live credentials. No hosting configuration is changed by this file.

## Frontend (Vercel)

```dotenv
API_BASE_URL=https://REPLACE_BACKEND_HOST
NEXT_PUBLIC_APP_NAME=MeetGrid
NEXT_PUBLIC_APP_TAGLINE="Good company. Better coordination."
NEXT_PUBLIC_SUPPORT_EMAIL=vivekgotstack@gmail.com
NEXT_PUBLIC_SUPPORT_PHONE=8303165648
NEXT_PUBLIC_COMPANY_NAME=StackOrcs
NEXT_PUBLIC_COMPANY_URL=https://stackorcs.com
```

`API_BASE_URL` is the backend origin, without `/api`. Public branding is a build-time fallback; saved admin content takes precedence. No frontend Stripe or authentication secret is needed.

## Backend (VPS with PostgreSQL)

```dotenv
PORT=8080
BIND_ADDRESS=127.0.0.1
SPRING_PROFILES_ACTIVE=postgres
DB_URL=jdbc:postgresql://REPLACE_DB_HOST:5432/meetgrid
DB_USERNAME=meetgrid
DB_PASSWORD=REPLACE_DATABASE_PASSWORD
APP_URL=https://REPLACE_FRONTEND_HOST
SESSION_TIMEOUT=8h
SESSION_COOKIE_SECURE=true
DEMO_RESET_ENABLED=false
ADMIN_EMAIL=vivekni1224@nigam
ADMIN_BOOTSTRAP_PASSWORD=REPLACE_WITH_16_TO_72_CHARACTER_SECRET
STRIPE_SECRET_KEY=REPLACE_STRIPE_SECRET_KEY
STRIPE_WEBHOOK_SECRET=REPLACE_STRIPE_WEBHOOK_SIGNING_SECRET
STRIPE_PRICE_STARTER=REPLACE_PRICE_ID_FOR_GATHER_7_USD_MONTHLY
STRIPE_PRICE_STUDIO=REPLACE_PRICE_ID_FOR_STUDIO_12_USD_MONTHLY
STRIPE_PRICE_SCALE=REPLACE_PRICE_ID_FOR_COLLECTIVE_20_USD_MONTHLY
```

Use `BIND_ADDRESS=0.0.0.0` for a container or platform that routes into the container. `127.0.0.1` is appropriate for a native VPS process behind a reverse proxy on the same machine. Use the port expected by your host.

Spring Boot reads the process environment, not `.env` files automatically. Pass the backend values through your process manager or Docker `env_file`. `APP_URL` is the frontend origin with no trailing slash.

Stripe checkout uses the backend secret key. Configure recurring monthly USD Price IDs for $7, $12, and $20, and enable the Stripe customer portal. Register your backend `/api/billing/webhook` for `customer.subscription.created`, `customer.subscription.updated`, and `customer.subscription.deleted`, then copy that endpoint's signing secret into `STRIPE_WEBHOOK_SECRET`. Leave all five Stripe values empty to keep checkout unavailable until configured. Live payments still need a Stripe sandbox verification.

The bootstrap password creates the owner account only once. Changing the environment value later does **not** rotate an existing administrator's password. You can remove this environment variable after the owner is created. Authentication uses server sessions; there is no JWT secret, SMTP key, Google key, or frontend publishable Stripe key in the current implementation.
