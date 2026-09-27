"use client";
import { useCallback, useEffect, useState } from "react";
import { ArrowRight, Check, ChevronDown, Clock3, DoorOpen, FlaskConical, HelpCircle, LoaderCircle, Minus, Plus, RotateCcw, Search, SlidersHorizontal, Users, X, ArrowUpRight, CalendarDays, CircleAlert } from "lucide-react";
import { api } from "@/lib/api";
import { type Availability, type Booking, type BookingInput, type Day, type Member, type MeetingOption, type Room, type SearchResult } from "@/lib/types";
import { AvailabilityEditor } from "./availability-editor";
import { BookingDialog } from "./booking-dialog";
import { Modal } from "./modal";
import { Timeline } from "./timeline";
import { Results } from "./results";
import { RoomDirectory } from "./room-directory";

export default function MeetGrid() {
  const [members, setMembers] = useState<Member[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [result, setResult] = useState<SearchResult | null>(null);
  const [duration, setDuration] = useState(60);
  const [capacity, setCapacity] = useState(4);
  const [submitted, setSubmitted] = useState({duration:60,capacity:4});
  const [day, setDay] = useState<Day>("TUESDAY");
  const [selected, setSelected] = useState("");
  const [tab, setTab] = useState<"planner" | "rooms">("planner");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [toast, setToast] = useState("");
  const [editing, setEditing] = useState<Member | null>(null);
  const [booking, setBooking] = useState<Partial<BookingInput> | null>(null);
  const [info, setInfo] = useState(false);
  const [resetting, setResetting] = useState(false);
  const stale = duration !== submitted.duration || capacity !== submitted.capacity;

  const acceptResults = useCallback((next: SearchResult, moveDay = false) => {
    setResult(next);
    setSelected(old => next.options.some(o => o.id === old) ? old : next.options[0]?.id || "");
    if (moveDay && next.options[0]) setDay(next.options[0].dayOfWeek);
  }, []);

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const [people, spaces, reservations] = await Promise.all([api.members(),api.rooms(),api.bookings()]);
      const found = await api.search(people.map(m => m.id),60,Math.max(4,people.length));
      setMembers(people); setRooms(spaces); setBookings(reservations); acceptResults(found, true);
      setDuration(60); setCapacity(Math.max(4,people.length)); setSubmitted({duration:60,capacity:Math.max(4,people.length)});
    } catch (e) { setError(e instanceof Error ? e.message : "Could not load your workspace."); }
    finally { setLoading(false); }
  }, [acceptResults]);
  useEffect(() => { void load(); }, [load]);
  useEffect(() => { if (!toast) return; const timeout = setTimeout(() => setToast(""),6000); return () => clearTimeout(timeout); }, [toast]);

  async function search() {
    setBusy(true); setError("");
    try {
      const found = await api.search(members.map(m => m.id),duration,capacity);
      acceptResults(found,true); setSubmitted({duration,capacity});
      setToast(found.options.length ? `${found.options.length} meeting options. People, rooms, and timing all checked.` : "Search complete. Try adjusting your constraints.");
    } catch(e) { setError(e instanceof Error ? e.message : "Search failed."); }
    finally { setBusy(false); }
  }
  async function saveAvailability(availability: Availability[]) {
    if (!editing) return;
    const updated = await api.availability(editing.id,availability);
    setMembers(old => old.map(m => m.id === updated.id ? updated : m));
    try {
      const found = await api.search(members.map(m => m.id),duration,capacity);
      acceptResults(found); setSubmitted({duration,capacity}); setToast(`${updated.name}’s availability saved. Your options are up to date.`);
    } catch {
      setResult(null); setError("Availability was saved, but results could not refresh. Please search again.");
    }
  }
  async function addBooking(input: BookingInput) {
    const saved = await api.book(input);
    setBookings(old => [...old,saved]);
    try {
      const found = await api.search(members.map(m => m.id),duration,capacity);
      const before = result?.options.length || 0;
      acceptResults(found); setSubmitted({duration,capacity});
      setToast(found.options.length < before ? `Booking added. ${before - found.options.length} meeting option removed; ${found.options.length} still available.` : "Booking added. Room availability has been recalculated.");
    } catch {
      setResult(null); setError("The booking was added, but results could not refresh. Please search again.");
    }
  }
  function select(option: MeetingOption) { setSelected(option.id); setDay(option.dayOfWeek); }
  async function reset() {
    setBusy(true); setError("");
    try { await api.reset(); setResetting(false); await load(); setToast("A fresh start. Original demo availability and bookings restored."); }
    catch(e) { setError(e instanceof Error ? e.message : "Reset failed."); }
    finally { setBusy(false); }
  }
  const active = result?.options.find(o => o.id === selected);

  return <div className="app-shell">
    <header className="topbar"><div className="topbar-inner">
      <a href="/" className="brand" aria-label="MeetGrid home"><span className="brand-mark"><i/><i/><i/><i/></span>meetgrid<span className="brand-period">.</span></a>
      <nav className="main-nav" aria-label="Workspace"><button className={tab === "planner" ? "active" : ""} onClick={() => setTab("planner")}><CalendarDays size={16}/>Meeting planner</button><button className={tab === "rooms" ? "active" : ""} onClick={() => setTab("rooms")}><DoorOpen size={16}/>Room directory</button></nav>
      <div className="topbar-actions"><span className="demo-badge"><i/>Demo workspace</span><button className="icon-button help-button" aria-label="How MeetGrid works" onClick={() => setInfo(true)}><HelpCircle size={20}/></button></div>
    </div></header>
    <main className="main-container">
      <section className="hero"><div><div className="hero-eyebrow"><span/>LESS COORDINATING. MORE CREATING.</div><h1>Good ideas need <em>common ground.</em></h1><p>Find the time. Find the room. One search.</p></div>
        <div className="hero-visual" aria-hidden="true"><div className="visual-people"><span className="avatar sage">V</span><span className="avatar lavender">R</span><span className="avatar apricot">A</span><span className="avatar blue">P</span></div><div className="visual-path"/><div className="visual-check"><Check size={20}/></div><div className="visual-path"/><div className="visual-room"><DoorOpen size={25}/></div><span className="visual-caption">Everyone. Somewhere. Together.</span></div>
      </section>
      {error && <div className="error-banner" role="alert"><CircleAlert size={18}/><span>{error}</span><button onClick={() => void (members.length ? search() : load())}>Try again</button><button className="icon-button small" aria-label="Dismiss error" onClick={() => setError("")}><X size={16}/></button></div>}
      {loading ? <div className="loading-workspace" role="status"><span className="brand-mark"><i/><i/><i/><i/></span><h2>Finding your common ground…</h2><p>Loading your team, rooms, and weekly availability.</p><LoaderCircle className="spin" size={22}/></div> : !members.length ? <div className="empty-state"><DoorOpen size={30}/><h2>Your workspace is taking a moment.</h2><p>Make sure the MeetGrid service is running, then try again.</p><button className="button primary" onClick={() => void load()}>Reload workspace</button></div> : tab === "rooms" ? <RoomDirectory rooms={rooms} bookings={bookings} onBook={roomId => setBooking({roomId})}/> :
      <div className="workspace-grid">
        <aside className="setup-column">
          <section className="panel setup-panel" aria-labelledby="setup-title">
            <div className="setup-heading"><span className="soft-icon"><SlidersHorizontal size={18}/></span><h2 id="setup-title">Make it work for you</h2></div>
            <p className="setup-subtitle">A few details. A lot less back-and-forth.</p>
            <label className="field-label" htmlFor="duration"><Clock3 size={15}/>Meeting duration</label>
            <div className="select-wrap"><select id="duration" value={duration} onChange={e => setDuration(Number(e.target.value))} disabled={busy}>{[15,30,45,60,90,120,180,240].map(n => <option key={n} value={n}>{n} minutes{n === 60 ? " · the sweet spot" : ""}</option>)}</select><ChevronDown size={15}/></div>
            <label className="field-label" htmlFor="capacity"><Users size={15}/>Seats you’ll need</label>
            <div className="capacity-control"><button aria-label="Decrease required seats" disabled={capacity <= members.length || busy} onClick={() => setCapacity(Math.max(members.length,capacity - 1))}><Minus size={16}/></button><input id="capacity" type="number" min={members.length} max={1000} value={capacity} disabled={busy} onChange={e => setCapacity(Math.min(1000,Math.max(members.length,Number(e.target.value) || members.length)))}/><span>people</span><button aria-label="Increase required seats" disabled={capacity >= 1000 || busy} onClick={() => setCapacity(capacity + 1)}><Plus size={16}/></button></div>
            <p className="input-hint">Your team of {members.length}, plus any extra guests.</p>
            <div className="team-heading"><span className="field-label">The people who make it happen</span><span className="count-badge">{members.length}</span></div>
            <div className="team-list">{members.map(member => <button key={member.id} className="member-row" onClick={() => setEditing(member)} disabled={busy} aria-label={`Edit ${member.name} availability`}><span className={`avatar ${member.color}`}>{member.name[0]}</span><span className="member-copy"><strong>{member.name}</strong><span>{new Set(member.availability.map(a => a.dayOfWeek)).size} days of availability</span></span><span className="member-edit">Edit<ArrowUpRight size={13}/></span></button>)}</div>
            <button className="button primary search-button" onClick={() => void search()} disabled={busy}>{busy ? <LoaderCircle size={18} className="spin"/> : <Search size={18}/>}Find Meeting Options<ArrowRight size={17}/></button>
            <span className="search-caption">Real overlap. Real rooms. Zero guesswork.</span>
          </section>
          <section className="demo-panel"><div className="demo-panel-label"><FlaskConical size={16}/><span>THE WHAT-IF CORNER</span></div><h3>Plans change.<br/>Your options should, too.</h3><p>Book a room and see your matches adapt in real time.</p><button className="button demo-button" onClick={() => setBooking({})} disabled={busy}>Try a room conflict<ArrowUpRight size={16}/></button><button className="reset-link" onClick={() => setResetting(true)} disabled={busy}><RotateCcw size={12}/>Reset demo data</button></section>
        </aside>
        <div className="results-column"><Timeline members={members} day={day} setDay={setDay} common={result?.commonIntervals || []} selected={active} stale={stale}/><Results result={result} selected={selected} onSelect={select} duration={submitted.duration} capacity={submitted.capacity} busy={busy} stale={stale}/><div className="constraint-note"><span className="constraint-symbol">=</span><p>People’s availability <b>+</b> open rooms <b>+</b> enough seats <b>+</b> enough time<br/><strong>A meeting that actually works.</strong></p><span className="tiny-grid" aria-hidden="true"><i/><i/><i/><i/></span></div></div>
      </div>}
      <footer className="footer"><span>Made for the moments you make together.</span><span><span className="footer-dot"/>MeetGrid<span className="footer-separator">/</span>A little less “when are you free?”</span></footer>
    </main>
    {toast && <div className="toast" role="status"><span className="toast-icon"><Check size={15}/></span>{toast}<button className="icon-button small" aria-label="Dismiss update" onClick={() => setToast("")}><X size={15}/></button></div>}
    {editing && <AvailabilityEditor member={editing} onClose={() => setEditing(null)} onSave={saveAvailability}/>}
    {booking && <BookingDialog rooms={rooms} initial={booking} onClose={() => setBooking(null)} onSave={addBooking}/>}
    {resetting && <Modal title="Back to a clean slate?" subtitle="Restore the four students, their original availability, and the seeded room bookings. Changes made in this demo will be cleared." onClose={() => setResetting(false)} busy={busy}><div className="modal-actions"><button className="button secondary" onClick={() => setResetting(false)} disabled={busy}>Keep my changes</button><button className="button primary" onClick={() => void reset()} disabled={busy}>{busy && <LoaderCircle size={16} className="spin"/>}Reset demo</button></div></Modal>}
    {info && <Modal title="Two puzzles. One perfect fit." subtitle="MeetGrid finds the time and the space for your whole team." onClose={() => setInfo(false)}><div className="how-it-works"><div><span>01</span><div><h3>Start with your people</h3><p>Set the duration and seats, then edit each student’s free periods.</p></div></div><div><span>02</span><div><h3>Find the common ground</h3><p>We intersect everyone’s availability, then check room hours, capacity, and existing bookings. Every result satisfies all four constraints.</p></div></div><div><span>03</span><div><h3>Put it to the test</h3><p>Add a Tuesday booking for Lab 2 from 2–3 PM. The Tuesday option disappears because no suitable room remains. Reset the demo to try again.</p></div></div></div><button className="button primary full-width" onClick={() => setInfo(false)}>Let’s find a time<ArrowRight size={16}/></button></Modal>}
  </div>;
}
