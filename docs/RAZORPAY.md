# Razorpay recurring billing

MeetGrid uses Razorpay Subscriptions with Standard Web Checkout. Orders are one-time payments and are not used for these automatically renewing plans. The existing Spring Boot HTTP client makes server-to-server API calls; no Node payment SDK or frontend secret is needed.

## Configuration

Remove STRIPE_SECRET_KEY, STRIPE_WEBHOOK_SECRET, STRIPE_PRICE_STARTER, STRIPE_PRICE_STUDIO, STRIPE_PRICE_SCALE from the backend host. Add:

```dotenv
RAZORPAY_KEY_ID=REPLACE_KEY_ID
RAZORPAY_KEY_SECRET=REPLACE_KEY_SECRET
RAZORPAY_WEBHOOK_SECRET=REPLACE_SEPARATE_RANDOM_WEBHOOK_SECRET
RAZORPAY_PLAN_STARTER=REPLACE_PLAN_ID_299_INR_MONTHLY
RAZORPAY_PLAN_STUDIO=REPLACE_PLAN_ID_599_INR_MONTHLY
RAZORPAY_PLAN_SCALE=REPLACE_PLAN_ID_999_INR_MONTHLY
RAZORPAY_SUBSCRIPTION_CYCLES=120
```

Enable Subscriptions, create monthly interval-1 INR plans with amounts 29900, 59900, 99900 paise, and supply their plan_ IDs. Use matching test keys/test plans initially; live keys require live plans. The public key ID is returned by the backend checkout response. There is no NEXT_PUBLIC_RAZORPAY_KEY_ID requirement. Existing DB, Redis, Brevo, Clerk, APP_URL, and proxy/session environment variables remain required as previously documented. These example files contain placeholders only.

Register `https://YOUR_BACKEND_HOST/api/billing/webhook` with the separate webhook secret. Subscribe to subscription.authenticated, activated, charged, pending, halted, paused, resumed, cancelled, completed, and updated as available. Restart the backend after configuration and redeploy the frontend.

## App flow and endpoints

Sign in with a verified account → Plans & billing → Choose a plan → review and authorize the Razorpay mandate. Checkout describes the monthly amount and maximum number of cycles. The backend checks ownership, saved subscription ID, signature, payment status, and the provider's current subscription before updating access. An authorized mandate without a charged cycle remains Preview. The service never trusts a client-provided price or an old webhook snapshot.

- POST /api/billing/checkout `{ "plan": "starter" }` creates or resumes an unfinished subscription. Account locking prevents simultaneous checkout creation.
- POST /api/billing/verify accepts razorpay_subscription_id, razorpay_payment_id, razorpay_signature. HMAC is over payment ID + `|` + the subscription ID saved by the backend.
- GET /api/billing/status reads saved billing state.
- POST /api/billing/refresh fetches current provider state and updates access.
- POST /api/billing/cancel cancels future renewal at an active cycle's end; an unfinished/inactive subscription is cancelled immediately.
- POST /api/billing/change `{ "plan": "studio" }` schedules a supported active card subscription change for the next cycle. Other payment methods can cancel and resubscribe after the paid period; provider restrictions are surfaced as errors.
- POST /api/billing/cancel-change removes a pending plan change. Cancelling renewal also removes a pending change before scheduling cancellation.
- POST /api/billing/webhook receives signed provider events without browser CSRF/session credentials.

New billing tables are created by Flyway V7 in PostgreSQL. Existing workspace records and historical database migrations are preserved. Previously saved provider identifiers do not confer Razorpay checkout ownership. Existing real mandates with a previous provider need cancellation in that provider's dashboard. Default legal copy names Razorpay; separately saved admin policy overrides need review.

## Verification before live launch

1. Use test keys/plans and the [official subscription test guide](https://razorpay.com/docs/payments/subscriptions/test/). Create a verified normal account; administrator workspaces need no paid plan.
2. Confirm the modal displays the selected amount and an automatic monthly mandate. A failed payment or dismissal must not unlock paid access. Continue a dismissed checkout without creating another subscription.
3. Complete authorization, allow the first charge, and refresh billing. Confirm paid access appears only after a confirmed charged cycle.
4. Deliver signed test webhooks. Repeat an event and deliver an older event after a newer one; access must reflect the provider's current state. Missing/invalid signatures must return 400.
5. Schedule cancellation: paid access continues through the saved paid period and then falls back to Preview. Test a failed renewal: it must not extend access or delete existing data.
6. Test a card-plan change at the next billing cycle and provider restrictions for other methods. Test tampered prices/signatures and another account's subscription ID.

The unit checks use mocked provider responses and never create a real payment. Docker-dependent integration checks need PostgreSQL/Redis containers. Browser payment completion, test-mode webhook delivery, merchant eligibility, provider notifications, tax/accounting setup, refunds and live mandates still need your provider account. The app does not execute refunds; billing support reviews requests through the existing contact workflow.

References: [Subscription checkout and HMAC](https://razorpay.com/docs/payments/subscriptions/integration-guide/), [webhook validation](https://razorpay.com/docs/webhooks/validate-test/), [subscription updates and restrictions](https://razorpay.com/docs/api/payments/subscriptions/update-subscription/).
