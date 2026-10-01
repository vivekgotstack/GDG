"use client";
import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import Script from 'next/script';
import { Check, ArrowUpRight, LoaderCircle, CreditCard } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Modal } from '@/components/modal';
import { request } from '@/lib/api';
import type { Plan } from '@/lib/product';
import type { BillingCheckout, BillingSubscription, RazorpayResult } from '@/lib/razorpay';
import { useSession } from './session';
import { SiteText, useSite } from './site-provider';
import { PublicNav, PublicFooter } from './landing';

type Usage = { plan: Plan; members: number; rooms: number; bookings: number; presets: number };
const terminal = (s: BillingSubscription) => ['cancelled', 'completed', 'expired'].includes(s.status);
const date = (value: string | null) => value ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value)) : 'Awaiting confirmation';
const money = (amount: number, currency: string) => new Intl.NumberFormat(currency === 'INR' ? 'en-IN' : 'en-US', { style: 'currency', currency, maximumFractionDigits: 0 }).format(amount);
const featureLabels = { bulk_import: 'Bulk people import', insights: 'Room utilization insights', operations_reports: 'Weekday heatmap, free-slot analysis & operations CSV' };

export function Pricing({ embedded = false }: { embedded?: boolean }) {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [usage, setUsage] = useState<Usage | null>(null);
  const [subscription, setSubscription] = useState<BillingSubscription | null>(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState('');
  const [scriptReady, setScriptReady] = useState(false);
  const [confirmation, setConfirmation] = useState<'cancel' | 'cancel-change' | Plan | null>(null);
  const { user, refresh } = useSession();
  const { brand } = useSite();

  useEffect(() => {
    let active = true;
    request<Plan[]>('/plans').then(data => { if (active) setPlans(data); }).catch(e => { if (active) setError(e.message); });
    return () => { active = false; };
  }, []);
  useEffect(() => {
    if (!embedded || !user?.id) return;
    let active = true;
    request<Usage>('/tools/usage').then(data => { if (active) setUsage(data); }).catch(e => { if (active) setError(e.message); });
    return () => { active = false; };
  }, [embedded, user?.id, user?.plan]);
  useEffect(() => {
    if (!user?.id) { setSubscription(null); return; }
    let active = true;
    request<{ subscription?: BillingSubscription }>('/billing/status').then(data => { if (active) setSubscription(data.subscription ?? null); }).catch(e => { if (active) setError(e.message); });
    return () => { active = false; };
  }, [user?.id]);

  const refreshBilling = useCallback(async () => {
    const result = await request<{ subscription?: BillingSubscription }>('/billing/refresh', { method: 'POST' });
    setSubscription(result.subscription ?? null);
    await refresh();
  }, [refresh]);
  useEffect(() => {
    if (!subscription || !['authenticated', 'active'].includes(subscription.status) || (subscription.paidUntil && new Date(subscription.paidUntil).getTime() > Date.now())) return;
    let attempts = 0, running = false;
    const timer = setInterval(() => {
      if (running) return;
      if (++attempts > 10) { clearInterval(timer); return; }
      running = true; void refreshBilling().catch(() => {}).finally(() => { running = false; });
    }, 6000);
    return () => clearInterval(timer);
  }, [subscription?.subscriptionId, subscription?.status, subscription?.paidUntil, refreshBilling]);

  async function openCheckout(plan: Plan) {
    if (!user || !window.Razorpay || busy) return;
    setBusy(plan.id); setError(''); setNotice('');
    try {
      const checkout = await request<BillingCheckout>('/billing/checkout', { method: 'POST', body: JSON.stringify({ plan: plan.id }) });
      let returned = false;
      const modal = new window.Razorpay({
        key: checkout.keyId,
        subscription_id: checkout.subscriptionId,
        name: brand.name,
        description: `${checkout.planName}: ${money(checkout.amount / 100, checkout.currency)} monthly, up to ${checkout.cycles} billing cycles`,
        image: `${window.location.origin}/icons/icon-192.png`,
        prefill: { name: user.name, email: user.email },
        theme: { color: '#6756A7' },
        handler: (result: RazorpayResult) => {
          returned = true;
          void (async () => {
            try {
              const status = await request<BillingSubscription>('/billing/verify', { method: 'POST', body: JSON.stringify(result) });
              setSubscription(status);
              await refresh();
              setNotice(status.paidUntil && new Date(status.paidUntil).getTime() > Date.now()
                ? 'Your payment is confirmed and your plan is ready.'
                : 'Your authorization is verified. Waiting for the first monthly charge; use Refresh billing status shortly.');
            } catch (e) {
              setError(`${e instanceof Error ? e.message : 'Payment confirmation is delayed.'} Use Refresh billing status before attempting another payment.`);
            } finally { setBusy(''); }
          })();
        },
        modal: {
          confirm_close: true,
          ondismiss: () => {
            if (!returned) { setBusy(''); setNotice('Checkout closed. You can continue the same checkout or cancel it in Plans & billing.'); void refreshBilling().catch(() => {}); }
          },
        },
      });
      modal.on('payment.failed', () => setError('Razorpay could not complete this payment. Try another supported method in the modal, or close it and refresh billing status.'));
      modal.open();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Checkout is unavailable.');
      setBusy('');
    }
  }

  async function confirmChange() {
    if (!confirmation) return;
    setBusy('manage'); setError(''); setNotice('');
    try {
      const cancelling = typeof confirmation === 'string';
      const result = await request<BillingSubscription>(cancelling ? `/billing/${confirmation}` : '/billing/change', {
        method: 'POST', ...(typeof confirmation === 'object' ? { body: JSON.stringify({ plan: confirmation.id }) } : {}),
      });
      setSubscription(result); await refresh(); setConfirmation(null);
      setNotice(confirmation === 'cancel-change' ? 'The scheduled plan change is cancelled.' : cancelling
        ? result.cancelAtPeriodEnd ? `Renewal is cancelled. Your paid access continues until ${date(result.paidUntil)}.` : 'The subscription is cancelled.'
        : 'Your plan change is scheduled for the next billing cycle.');
    } catch (e) { setError(e instanceof Error ? e.message : 'Billing could not be updated.'); }
    finally { setBusy(''); }
  }

  function planAction(plan: Plan) {
    if (!user) return <Button asChild variant={plan.id === 'studio' ? 'default' : 'outline'} className="full-width"><Link href={`/signup?plan=${plan.id}`}>Explore {plan.name}<ArrowUpRight size={16}/></Link></Button>;
    if (user.role === 'ADMIN') return <Button className="full-width" variant="outline" disabled>Admin access included</Button>;
    if (subscription && !terminal(subscription) && subscription.status !== 'created') {
      if (subscription.planId === plan.id) return <Button variant="outline" className="full-width" disabled>Current plan</Button>;
      if (subscription.pendingPlanId === plan.id) return <Button variant="outline" className="full-width" disabled>Change scheduled</Button>;
      return <Button className="full-width" variant="outline" disabled={!!busy || !subscription.canChangePlan || !plan.checkoutEnabled} onClick={() => setConfirmation(plan)}>Change to {plan.name}</Button>;
    }
    const anotherCheckout = subscription?.status === 'created' && subscription.planId !== plan.id;
    return <Button className="full-width" variant={plan.id === 'studio' ? 'default' : 'outline'} disabled={!!busy || !plan.checkoutEnabled || !scriptReady || anotherCheckout} onClick={() => void openCheckout(plan)}>
      {busy === plan.id && <LoaderCircle size={15} className="spin"/>}
      {!plan.checkoutEnabled ? 'Checkout opening soon' : !scriptReady ? 'Loading checkout…' : subscription?.status === 'created' ? 'Continue checkout' : `Choose ${plan.name}`}
    </Button>;
  }

  return <div className={embedded ? 'pricing-embedded' : 'public-site'}>
    {user && plans.some(plan => plan.checkoutEnabled) && (
      <Script
        id="razorpay-checkout"
        src="https://checkout.razorpay.com/v1/checkout.js"
        strategy="afterInteractive"
        onReady={() => setScriptReady(true)}
        onError={() => {
          setScriptReady(false);
          setError('Razorpay checkout could not load. Check your connection or browser blocker, then reload.');
        }}
      />
    )}
    {!embedded && <PublicNav/>}
    <section id="main-content" className="pricing-section">
      <span className="kicker">A LITTLE STRUCTURE. A LOT OF POSSIBILITY.</span><h1><SiteText name="pricing.heading"/></h1><p><SiteText name="pricing.intro"/></p>
      {error && <p role="alert" className="form-error">{error}</p>}
      {notice && <p role="status" className="success-note">{notice}</p>}
      {embedded && usage && <div className="usage-summary"><div><strong>{user?.role === 'ADMIN' ? 'Administrator workspace' : `${usage.plan.name} workspace`}</strong><span>{user?.role === 'ADMIN' ? 'Administrative workspaces have no plan caps.' : 'Your current usage, updated from saved records.'}</span></div>{(['members', 'rooms', 'bookings', 'presets'] as const).map(key => <span key={key}><strong>{usage[key]}</strong> / {user?.role === 'ADMIN' ? '∞' : usage.plan[key]} {key}</span>)}</div>}
      {embedded && subscription && <Card className="billing-status-card"><CardContent><div className="billing-status-heading"><CreditCard size={23}/><div><h2>Your Razorpay subscription</h2><p>{plans.find(p => p.id === subscription.planId)?.name ?? subscription.planId} · {money(subscription.amount / 100, subscription.currency)} / month</p></div><Badge>{subscription.status}</Badge></div><dl className="billing-status-details"><div><dt>Paid access until</dt><dd>{date(subscription.paidUntil)}</dd></div><div><dt>Renewal</dt><dd>{subscription.cancelAtPeriodEnd ? 'Cancelled after this cycle' : terminal(subscription) ? 'Ended' : subscription.status === 'created' ? 'Checkout not completed' : 'Automatic monthly'}</dd></div><div><dt>Subscription reference</dt><dd>{subscription.subscriptionId}</dd></div>{subscription.lastPaymentId && <div><dt>Last checkout payment</dt><dd>{subscription.lastPaymentId}</dd></div>}</dl>{subscription.pendingPlanId && <p>Changing to {plans.find(p => p.id === subscription.pendingPlanId)?.name ?? subscription.pendingPlanId} at the next billing cycle.</p>}{!terminal(subscription) && !subscription.canChangePlan && subscription.status !== 'created' && <p>For a different plan, cancel renewal and choose the new plan after your paid period. In-place changes depend on the payment method.</p>}</CardContent></Card>}
      {!plans.length && !error && <p role="status">Loading plans…</p>}
      <div className="pricing-grid">{plans.map(plan => <Card key={plan.id} className={`pricing-card ${plan.id === 'studio' ? 'featured-plan' : ''}`}><CardContent><div className="plan-label"><h2>{plan.name}</h2>{plan.id === 'studio' && <Badge>Room to grow</Badge>}</div><p>{plan.description}</p><div className="plan-price">{money(plan.monthlyPrice, plan.currency)}<span>{plan.currency} / workspace / month</span></div><ul>{[`${plan.members} people in your directory`, `${plan.rooms} bookable rooms`, `${plan.bookings} active weekly reservations`, `${plan.presets} saved meeting presets`, 'Availability matching & conflict checks', 'Workspace, CSV & calendar export', ...Object.entries(featureLabels).filter(([key]) => plan.features?.includes(key)).map(([, label]) => label)].map(item => <li key={item}><Check size={15}/>{item}</li>)}</ul>{planAction(plan)}</CardContent></Card>)}</div>
      {!!plans.length && <div className="plan-comparison report-scroll"><table><caption>What changes with each plan</caption><thead><tr><th scope="col">Workspace tools</th>{plans.map(plan => <th scope="col" key={plan.id}>{plan.name}</th>)}</tr></thead><tbody><tr><th scope="row">Scheduling, conflict checks & data export</th>{plans.map(plan => <td key={plan.id}>Included</td>)}</tr>{Object.entries(featureLabels).map(([key, label]) => <tr key={key}><th scope="row">{label}</th>{plans.map(plan => <td key={plan.id}>{plan.features?.includes(key) ? 'Included' : '—'}</td>)}</tr>)}</tbody></table></div>}
      <p className="pricing-footnote">Preview includes 5 people, 2 rooms, 10 weekly bookings, and 3 presets with no card required. Scheduling and data export are available in Preview. Gather unlocks bulk import; Studio adds utilization insights; Collective adds operations reports. Paid plans renew automatically each month through Razorpay until cancellation or the authorized cycle limit. Review the amount and mandate details in checkout. Displayed prices exclude applicable taxes.</p>
      <div className="pricing-legal-links"><Link href="/refunds">Refunds & cancellation</Link><Link href="/terms">Terms</Link><Link href="/contact">Talk to us</Link></div>
      {user && <div className="billing-tools">
        {subscription && !terminal(subscription) && !subscription.cancelAtPeriodEnd && <Button variant="outline" disabled={!!busy} onClick={() => setConfirmation('cancel')}>{subscription.status === 'created' ? 'Cancel unfinished checkout' : 'Cancel renewal'}</Button>}
        {subscription?.pendingPlanId && <Button variant="outline" disabled={!!busy} onClick={() => setConfirmation('cancel-change')}>Cancel scheduled change</Button>}
        <Button variant="ghost" disabled={!!busy} onClick={async () => { setBusy('refresh'); setError(''); try { await refreshBilling(); setNotice('Billing status refreshed.'); } catch (e) { setError(e instanceof Error ? e.message : 'Could not refresh billing.'); } finally { setBusy(''); } }}>Refresh billing status</Button>
        {!embedded && <Button asChild variant="ghost"><Link href="/app/billing">Plans & billing<ArrowUpRight size={15}/></Link></Button>}
        <p>Access updates after the first confirmed charge. Existing records remain after cancellation or a smaller plan; new records must fit your available limits.</p>
      </div>}
      <div className="pricing-faq"><h2>Choose the space you need.</h2><details><summary>Which plan fits my group?</summary><p>Gather helps small clubs import a roster and coordinate weekly meetings. Studio adds room-utilization insights for teams managing several spaces. Collective adds a weekday heatmap, longest free windows, and downloadable operations reports for shared-space operators. Each tier also increases workspace capacity.</p></details><details><summary>Does each person get a login?</summary><p>Directory entries count the people whose availability you manage. They do not create accounts or invitations. Each account has its own private workspace.</p></details><details><summary>Can I explore before paying?</summary><p>Yes. Preview needs no card and never automatically becomes a paid subscription.</p></details><details><summary>Are bookings recurring?</summary><p>Availability and reservations repeat weekly. Calendar export produces recurring events in your workspace timezone.</p></details><details><summary>Can I change or cancel a plan?</summary><p>Manage renewal in Plans & billing. Active card subscriptions can schedule a plan change for the next cycle when supported. For other methods, cancel renewal and choose a new plan after the paid period. See <Link href="/refunds">refunds and cancellation</Link> for help.</p></details></div>
    </section>
    {!embedded && <PublicFooter/>}
    {confirmation && <Modal title={typeof confirmation === 'string' ? confirmation === 'cancel' ? 'Cancel renewal?' : 'Cancel scheduled change?' : `Change to ${confirmation.name}?`} subtitle="Review your billing change before confirming." busy={!!busy} onClose={() => { if (!busy) setConfirmation(null); }}>
      <div className="billing-confirmation">
        {confirmation === 'cancel-change' ? <p>Your current plan and monthly renewal stay in place. The pending plan change will be removed.</p>
          : confirmation === 'cancel' ? <p>{subscription?.status === 'active' ? `Your current paid access continues until ${date(subscription.paidUntil)}. Razorpay will stop future renewals.` : 'This stops the unfinished or inactive subscription. A completed charge is not refunded by this action.'}</p>
          : <p>Your next cycle will use {confirmation.name} at {money(confirmation.monthlyPrice, confirmation.currency)} per month. Current limits continue until the change takes effect.</p>}
        {error && <p role="alert" className="form-error">{error}</p>}
        <Button disabled={!!busy} onClick={() => void confirmChange()}>{busy && <LoaderCircle size={15} className="spin"/>}{typeof confirmation === 'string' ? 'Confirm cancellation' : 'Schedule plan change'}</Button>
        <Button variant="ghost" disabled={!!busy} onClick={() => setConfirmation(null)}>Keep current setup</Button>
      </div>
    </Modal>}
  </div>;
}
