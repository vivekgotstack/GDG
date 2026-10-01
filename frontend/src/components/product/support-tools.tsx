'use client';

import { useState } from 'react';
import { ArrowUpRight, Mail, Search } from 'lucide-react';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { useSite } from './site-provider';

export function ContactComposer() {
  const {brand:product}=useSite();
  const [draft, setDraft] = useState('');
  return <section className="contact-composer">
    <span className="kicker">A GOOD CONVERSATION STARTS HERE</span>
    <h2>What’s on your mind?</h2>
    <p>Write a little context. We’ll prepare an email for you to review and send from your email app.</p>
    <form className="product-form" onChange={() => setDraft('')} onSubmit={event => {
      event.preventDefault();
      const data = new FormData(event.currentTarget);
      const subject = `${product.name} · ${data.get('topic')}`;
      const body = `Hello ${product.name} team,\n\n${String(data.get('message')).trim()}\n\nName: ${String(data.get('name')).trim()}\nWorkspace (optional): ${String(data.get('workspace')).trim()}\n`;
      const href = `mailto:${product.supportEmail}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
      setDraft(href);
      window.location.assign(href);
    }}>
      <div className="contact-form-row"><div><Label htmlFor="contact-name">Your name</Label><Input id="contact-name" name="name" autoComplete="name" required maxLength={80} placeholder="How should we address you?"/></div><div><Label htmlFor="contact-workspace">Workspace <span>(optional)</span></Label><Input id="contact-workspace" name="workspace" maxLength={100} placeholder="Your team or organisation"/></div></div>
      <Label htmlFor="contact-topic">I’m reaching out about</Label>
      <select id="contact-topic" name="topic" defaultValue="Product support"><option>Product support</option><option>Billing or refund request</option><option>Privacy request</option><option>Security report</option><option>Accessibility feedback</option><option>Partnership or general enquiry</option></select>
      <Label htmlFor="contact-message">Your message</Label><Textarea id="contact-message" name="message" required minLength={10} maxLength={1500} rows={6} placeholder="Tell us what you need, and include the page or feature involved."/>
      <small className="field-note">Please leave out passwords, card numbers, and private member details. This draft stays in your browser until you choose to send it through your email app.</small>
      <Button type="submit">Open email draft <ArrowUpRight size={17}/></Button>
      {draft && <div className="contact-draft-note" role="status"><Mail size={18}/><div><strong>Your email draft is ready.</strong><p>It has not been sent by this website. Review and send it in your email app. If nothing opened, <a href={draft}>open the draft again</a> or email <a href={`mailto:${product.supportEmail}`}>{product.supportEmail}</a> directly.</p></div></div>}
    </form>
  </section>;
}

const helpAnswers = [
  { question: 'How do I get my first meeting booked?', answer: 'Create an account, open Meeting planner, and add your people and their weekly availability. Add at least one room with its seats and opening hours. Select the participants, day, duration, and capacity, then search. Book a matching result or use a room’s booking action when you already know the time.', category: 'Getting started', href: '/app/planner', action: 'Open the planner' },
  { question: 'Can I customise the names, people, rooms, and schedules?', answer: 'Yes. New accounts start with an empty workspace. You can add, edit, and remove directory members, their colours and availability, rooms, and meeting presets. Settings lets you change your name, workspace name, and timezone.', category: 'Your workspace', href: '/app/settings', action: 'Workspace settings' },
  { question: 'Does adding a member invite them or give them a login?', answer: 'No. The member directory represents people you coordinate. Adding someone does not send an invitation, create an account, or share access. Each signed-in account manages its own workspace. Ask people for their availability and only enter information you have authority to manage.', category: 'People & access' },
  { question: 'Are bookings one-off or recurring?', answer: 'Availability and reservations repeat every week, Monday through Sunday. Room opening hours apply to every day. Cancelling a booking removes that recurring reservation; individual date exceptions are not currently supported.', category: 'Scheduling', href: '/app/bookings', action: 'Manage bookings' },
  { question: 'Why am I not seeing any matching times?', answer: 'Check that every selected person has availability on that day, the requested duration fits the shared free time, and a room has enough seats and is open. Existing reservations can block an otherwise suitable room. Try a shorter duration, a different day, or a smaller participant group.', category: 'Scheduling', href: '/app/planner', action: 'Adjust your search' },
  { question: 'How do timezones and calendar exports work?', answer: 'All schedule times use your workspace timezone. Changing it relabels the existing times rather than converting them. Bookings can be exported as recurring ICS calendar events for import into a calendar application. Exports are snapshots; a later edit in MeetGrid does not update a previously imported event.', category: 'Calendars', href: '/app/bookings', action: 'View calendar exports' },
  { question: 'What are meeting presets and insights?', answer: 'Presets save a meeting name, description, duration, and capacity so you can reuse the setup. Applying a preset opens the planner; you still choose people and a time. Insights compares reserved room hours with configured weekly opening hours. It describes reservations, not measured attendance.', category: 'Product features', href: '/app/presets', action: 'Make a preset' },
  { question: 'How do plans, upgrades, and cancellation work?', answer: 'Explore a preview workspace without a card. Gather, Studio, and Collective expand directory, room, weekly booking, and preset limits. Plans & billing opens Razorpay Checkout for automatic monthly renewal and shows subscription status and cancellation controls. Supported card subscriptions can change plans at the next billing cycle; other methods may need cancellation and a new subscription after the paid period. Access starts after a confirmed charged cycle. Cancelling renewal keeps already paid access through its saved expiry date.', category: 'Billing', href: '/refunds', action: 'Read billing guidance' },
  { question: 'Why do I need to sign in again, and can I reset my password?', answer: 'Sessions expire after inactivity and can also end when the backend restarts. Sign in again to continue. Self-service password reset and email verification are not currently available. If you cannot sign in, contact support from your account email; never send your password.', category: 'Account access', href: '/contact', action: 'Get account help' },
  { question: 'How do I request my data or delete my account?', answer: 'You can edit or remove supported workspace records inside the application. For account deletion, a data copy, or another privacy request, email support with the subject “Privacy request” and identify the account or workspace involved. We may need to verify ownership. Signing out or cancelling billing does not delete your account.', category: 'Privacy', href: '/privacy', action: 'Read the privacy policy' },
];

export function HelpAnswers() {
  const {brand:product}=useSite();
  const [query, setQuery] = useState('');
  const visible = helpAnswers.filter(item => `${item.question} ${item.answer} ${item.category}`.toLowerCase().includes(query.trim().toLowerCase()));
  return <section className="help-answers" aria-labelledby="help-questions">
    <div className="help-search-heading"><div><span className="kicker">THE EVERYDAY QUESTIONS</span><h2 id="help-questions">A little clarity.</h2></div><div className="help-search"><Search size={17}/><Label htmlFor="help-search" className="sr-only">Search help articles</Label><Input id="help-search" type="search" value={query} onChange={e => setQuery(e.target.value)} placeholder="Try “bookings” or “timezone”"/></div></div>
    <p className="help-result-count" role="status">{query && `${visible.length} matching ${visible.length === 1 ? 'answer' : 'answers'}`}</p>
    <div className="help-answer-list">{visible.map(item => <details key={item.question} open={query.trim() ? true : undefined}><summary><span><small>{item.category}</small>{item.question}</span><span className="faq-plus" aria-hidden="true">+</span></summary><div><p>{item.answer.replaceAll('MeetGrid', product.name)}</p>{item.href && <Link href={item.href}>{item.action}<ArrowUpRight size={14}/></Link>}</div></details>)}</div>
    {!visible.length && <div className="help-no-results"><h3>No match yet.</h3><p>Try a shorter phrase, or tell us what you need.</p><Link href="/contact">Contact support <ArrowUpRight size={16}/></Link></div>}
  </section>;
}
