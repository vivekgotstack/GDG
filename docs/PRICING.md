# India launch pricing

## Enforced plan differences

| Plan | Monthly price | People / rooms / weekly bookings / presets | Additional tools |
| --- | --- | --- | --- |
| Preview | Free | 5 / 2 / 10 / 3 | Core scheduling, conflict checks, workspace/CSV/calendar export |
| Gather | ₹299 | 10 / 3 / 30 / 5 | Bulk people import |
| Studio | ₹599 | 30 / 10 / 150 / 25 | Gather tools plus room utilization, reserved hours, busiest weekday |
| Collective | ₹999 | 100 / 30 / 600 / 100 | Studio tools plus weekday heatmap, longest free windows, filtered operations CSV |

Prices and capacity limits come from PostgreSQL plan definitions and remain editable in Administration. Tools use the stable internal IDs `starter`, `studio`, and `scale`; changing a plan's display name does not change its tools. `/api/tools/entitlements` is the frontend's source of access. Import and both report endpoints check the effective paid tier on the backend. Capacity checks lock the account inside the write transaction, including the full size of a bulk import.

Paid access starts after a confirmed first charge, stays until the paid-through date after cancellation, and returns to Preview on expiry. Authorization alone grants no paid tools. Existing records remain readable, editable, deletable and exportable after a downgrade; new records must fit the new limits. Use a regular verified USER account when reviewing the tier differences: ADMIN workspaces intentionally include all tools without capacity caps.

Reports use saved recurring weekly reservations and configured room opening hours for all seven weekdays. They measure scheduled utilization, not attendance; overlapping intervals count once. Search, weekday filtering and pagination keep larger workspaces manageable. The operations CSV contains every filtered row, including rows beyond the current page.

Gather ₹299, Studio ₹599, Collective ₹999 per workspace per month, excluding applicable tax. Existing limits remain 10/30/100 people, 3/10/30 rooms, 30/150/600 weekly reservations, and 5/25/100 presets. Preview remains no-card and limited. The migration changes only the original USD defaults; customized plans are preserved. The admin can choose INR or USD, but Razorpay eligibility for a currency depends on the merchant account. Razorpay monthly Plan IDs must match the displayed currency and amount exactly. Subscriptions renew automatically until cancellation or their authorized cycle limit (default 120 months).

This is a launch hypothesis for campus groups, small teams, and shared-space operators, not a demonstrated optimum. MeetGrid offers weekly people-and-room matching; it does not yet match mature competitors' calendar sync, reminders, team-login collaboration, or scheduling-link breadth. [Cal.com](https://cal.com/pricing) offers a free individual tier and paid per-user team tiers; [Zoho Bookings](https://www.zoho.com/bookings/pricing.html) localizes prices for India. Per-workspace and per-user pricing are not directly comparable. A lower price alone does not create demand.

## Break-even worksheet (illustrative assumptions, not actual invoices)

Let F = monthly hosting, provider and operating costs; v = variable cost per paying workspace; f = payment-fee fraction; P = average realized subscription price after discounts and taxes.

Contribution per workspace = P × (1 − f) − v.
Break-even paying workspaces = ceiling(F / contribution), if contribution is positive.

At F=₹3,000, v=₹8, f=3%, with all customers on one tier, Gather contributes ₹282.03 and needs 11 customers; Studio contributes ₹573.03 and needs 6; Collective contributes ₹961.03 and needs 4. These assumptions exclude founder labour, support spikes, refunds, chargebacks, taxes, paid acquisition and provider minimums. Replace every assumption with actual costs before deciding profitability.

Track completed first booking, preview-to-paid conversion, monthly retention, support minutes, and net contribution. Review after a real cohort; avoid lifetime deals or unlimited promises until costs and retention are known. No revenue or profit guarantee is made.
