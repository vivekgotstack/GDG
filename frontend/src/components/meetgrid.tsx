"use client";
import { useCallback, useEffect, useState } from "react";
import { ArrowRight, Check, Clock3, DoorOpen, HelpCircle, LoaderCircle, Plus, Search, SlidersHorizontal, Users, X, CalendarDays, CircleAlert, Pencil, Trash2 } from "lucide-react";
import { Pagination,usePagination } from "./pagination";
import { Input } from "./ui/input";
import { api } from "@/lib/api";
import { dayName, range, type Availability, type Booking, type BookingInput, type Day, type Member, type MeetingOption, type Room, type SearchResult } from "@/lib/types";
import { AvailabilityEditor } from "./availability-editor";
import { BookingDialog } from "./booking-dialog";
import { MemberEditor, RoomEditor, ConfirmAction } from "./workspace-editors";
import { Modal } from "./modal";
import { Timeline } from "./timeline";
import { Results } from "./results";
import { RoomDirectory } from "./room-directory";
import { Button } from "./ui/button";
import Link from 'next/link';

type Confirmation = {title:string; description:string; label:string; action:()=>Promise<void>};
export default function MeetGrid({view='planner',initialDuration=60,initialCapacity=1}:{view?:'planner'|'rooms';initialDuration?:number;initialCapacity?:number}) {
  const [members,setMembers] = useState<Member[]>([]);
  const [rooms,setRooms] = useState<Room[]>([]);
  const [bookings,setBookings] = useState<Booking[]>([]);
  const [participantIds,setParticipantIds] = useState<string[]>([]);
  const [result,setResult] = useState<SearchResult|null>(null);
  const [duration,setDuration] = useState(initialDuration);
  const [capacity,setCapacity] = useState(initialCapacity);
  const [submitted,setSubmitted] = useState({duration:60,capacity:1,ids:""});
  const [day,setDay] = useState<Day>("MONDAY");
  const [selected,setSelected] = useState("");
  const tab = view;
  const [loading,setLoading] = useState(true);
  const [busy,setBusy] = useState(false);
  const [error,setError] = useState("");
  const [toast,setToast] = useState("");
  const [editing,setEditing] = useState<Member|null>(null);
  const [memberEditor,setMemberEditor] = useState<Member|"new"|null>(null);
  const [roomEditor,setRoomEditor] = useState<Room|"new"|null>(null);
  const [booking,setBooking] = useState<Partial<BookingInput>|null>(null);
  const [confirmation,setConfirmation] = useState<Confirmation|null>(null);
  const [info,setInfo] = useState(false);
  const group = members.filter(m=>participantIds.includes(m.id));
  const stale = duration!==submitted.duration || capacity!==submitted.capacity || participantIds.join(",")!==submitted.ids;

  const acceptResults = useCallback((found:SearchResult|null)=>{
    setResult(found); setSelected(found?.options[0]?.id || "");
    if(found?.options[0]) setDay(found.options[0].dayOfWeek);
  },[]);
  const syncWorkspace = useCallback(async (ids?:string[], seats=1, minutes=60)=>{
    const [people,spaces,reservations] = await Promise.all([api.members(),api.rooms(),api.bookings()]);
    const chosen = ids ? ids.filter(id=>people.some(m=>m.id===id)) : people.slice(0,50).map(m=>m.id);
    const requiredSeats = Math.max(seats,chosen.length,1);
    setMembers(people);setRooms(spaces);setBookings(reservations);setParticipantIds(chosen);setCapacity(requiredSeats);
    acceptResults(null);
    const found = chosen.length ? await api.search(chosen,minutes,requiredSeats) : null;
    acceptResults(found);setSubmitted({duration:minutes,capacity:requiredSeats,ids:chosen.join(",")});
  },[acceptResults]);
  const initialLoad = useCallback(async()=>{
    setLoading(true);setError("");
    try {await syncWorkspace(undefined,initialCapacity,initialDuration);setDuration(initialDuration);} catch(e) {setError(e instanceof Error ? e.message : "Could not load workspace.");} finally {setLoading(false);}
  },[syncWorkspace,initialCapacity,initialDuration]);
  useEffect(()=>{void initialLoad();},[initialLoad]);
  useEffect(()=>{if(!toast)return;const timeout=setTimeout(()=>setToast(""),6000);return()=>clearTimeout(timeout);},[toast]);
  async function refresh(message:string, ids=participantIds) {
    setBusy(true);setError("");
    try {await syncWorkspace(ids,capacity,duration);setToast(message);} catch {setResult(null);setError("Your change was saved, but the workspace could not refresh. Try again.");} finally {setBusy(false);}
  }
  async function search() {
    if(!participantIds.length)return;
    setBusy(true);setError("");
    try {const found=await api.search(participantIds,duration,capacity);acceptResults(found);setSubmitted({duration,capacity,ids:participantIds.join(",")});setToast(`${found.options.length} meeting options found.`);} catch(e){setError(e instanceof Error ? e.message : "Search failed.");}finally{setBusy(false);}
  }
  function toggleParticipant(id:string) {
    const next=participantIds.includes(id)?participantIds.filter(item=>item!==id):[...participantIds,id];
    setParticipantIds(next);setCapacity(value=>Math.max(value,next.length,1));
  }
  async function saveAvailability(availability:Availability[]) {
    if(!editing)return;
    await api.availability(editing.id,availability);await refresh(`${editing.name}'s availability saved.`);
  }
  async function addBooking(input:BookingInput) {await api.book(input);await refresh("Room booked. Meeting options updated.");}
  function select(option:MeetingOption) {setSelected(option.id);setDay(option.dayOfWeek);}
  function removeMember(member:Member) {
    setConfirmation({title:`Remove ${member.name}?`,description:"This removes the member and their weekly availability. Room bookings are kept.",label:"Remove member",action:async()=>{await api.deleteMember(member.id);await refresh("Member removed.",participantIds.filter(id=>id!==member.id));}});
  }
  function removeRoom(room:Room) {
    const count=bookings.filter(b=>b.roomId===room.id).length;
    setConfirmation({title:`Delete ${room.name}?`,description:`This removes the room and its ${count} weekly booking${count===1?"":"s"}. This cannot be undone.`,label:"Delete room",action:async()=>{await api.deleteRoom(room.id);await refresh("Room deleted.");}});
  }
  function cancelBooking(item:Booking) {
    setConfirmation({title:"Cancel this booking?",description:`${item.roomName} · ${dayName(item.dayOfWeek)} · ${range(item.startTime,item.endTime)}. The room will become available for this weekly slot.`,label:"Cancel booking",action:async()=>{await api.cancelBooking(item.id);await refresh("Booking cancelled.");}});
  }
  function loadSamples() {
    setConfirmation({title:"Replace workspace with sample data?",description:"All current members, availability, rooms, and bookings will be replaced with sample entries. This cannot be undone.",label:"Replace with samples",action:async()=>{await api.reset();await initialLoad();setToast("Sample workspace loaded. Every member and room is editable.");}});
  }
  const [peopleQuery,setPeopleQuery]=useState("");
  const people=usePagination(members.filter(m=>m.name.toLowerCase().includes(peopleQuery.toLowerCase())),8,peopleQuery);
  const active=result?.options.find(o=>o.id===selected);
  return <div className="app-shell planner-embedded">
    <a className="skip-link" href="#meeting-workspace">Skip to workspace</a>
    <header className="topbar"><div className="topbar-inner">
      <a href="/" className="brand" aria-label="MeetGrid home"><span className="brand-mark"><i/><i/><i/><i/></span>meetgrid<span className="brand-period">.</span></a>
      <nav className="main-nav" aria-label="Workspace"><Link className={tab==="planner"?"active":""} href="/app/planner"><CalendarDays size={16}/>Meeting planner</Link><Link className={tab==="rooms"?"active":""} href="/app/rooms"><DoorOpen size={16}/>Room directory</Link></nav>
      <div className="topbar-actions"><span className="demo-badge"><i/>Your workspace</span><button className="icon-button" aria-label="How MeetGrid works" onClick={()=>setInfo(true)}><HelpCircle size={20}/></button></div>
    </div></header>
    <main id="meeting-workspace" className="main-container">
      <section className="hero"><div><div className="hero-eyebrow"><span/>YOUR PEOPLE. YOUR SPACES. YOUR SCHEDULE.</div><h1>Good ideas need <em>common ground.</em></h1><p>Build your team. Add your rooms. Find a time that works.</p></div><div className="hero-visual" aria-hidden="true"><Users size={36}/><div className="visual-path"/><div className="visual-check"><Check size={20}/></div><div className="visual-path"/><div className="visual-room"><DoorOpen size={25}/></div></div></section>
      {error && <div className="error-banner" role="alert"><CircleAlert size={18}/><span>{error}</span><button onClick={()=>void refresh("Workspace refreshed.")}>Try again</button><button className="icon-button small" aria-label="Dismiss error" onClick={()=>setError("")}><X size={16}/></button></div>}
      {loading ? <div className="loading-workspace" role="status"><LoaderCircle className="spin" size={24}/><h2>Loading your workspace…</h2></div> : tab==="rooms" ?
        <RoomDirectory rooms={rooms} bookings={bookings} onBook={roomId=>setBooking({roomId})} onAdd={()=>setRoomEditor("new")} onEdit={setRoomEditor} onDelete={removeRoom} onCancel={cancelBooking}/> :
        <div className="workspace-grid"><aside className="setup-column">
          <section className="panel setup-panel" aria-labelledby="setup-title">
            <div className="setup-heading"><span className="soft-icon"><SlidersHorizontal size={18}/></span><h2 id="setup-title">Make it work for you</h2></div><p className="setup-subtitle">Choose the people and space you need.</p>
            <label className="field-label" htmlFor="duration"><Clock3 size={15}/>Meeting duration</label>
            <div className="capacity-control"><input id="duration" type="number" min={15} max={480} value={duration} onChange={e=>setDuration(Math.min(480,Math.max(15,Number(e.target.value)||15)))} disabled={busy}/><span>minutes</span></div>
            <label className="field-label" htmlFor="capacity"><Users size={15}/>Seats needed</label>
            <div className="capacity-control"><input id="capacity" type="number" min={Math.max(group.length,1)} max={1000} value={capacity} disabled={busy} onChange={e=>setCapacity(Math.min(1000,Math.max(group.length,1,Number(e.target.value)||1)))}/><span>people</span></div><p className="input-hint">{group.length} selected, plus any extra guests.</p>
            <div className="team-heading"><span className="field-label">Your team</span><Button variant="outline" size="sm" onClick={()=>setMemberEditor("new")} disabled={busy}><Plus size={14}/>Add member</Button></div>
            {!members.length && <p className="empty-team">Add your first member, then set their weekly availability.</p>}
            <Input aria-label="Search team members" placeholder="Find a person…" value={peopleQuery} onChange={e=>setPeopleQuery(e.target.value)}/><div className="team-list">{people.items.map(member=><div className="team-entry" key={member.id}>
              <div className="team-entry-top"><input type="checkbox" aria-label={`Include ${member.name} in meeting`} checked={participantIds.includes(member.id)} disabled={busy || (!participantIds.includes(member.id)&&participantIds.length>=50)} onChange={()=>toggleParticipant(member.id)}/><span className={`avatar ${member.color}`}>{member.name.slice(0,1).toUpperCase()}</span><strong title={member.name}>{member.name}</strong><button className="icon-button small" aria-label={`Edit ${member.name}`} onClick={()=>setMemberEditor(member)} disabled={busy}><Pencil size={14}/></button><button className="icon-button small danger" aria-label={`Remove ${member.name}`} onClick={()=>removeMember(member)} disabled={busy}><Trash2 size={14}/></button></div>
              <button className="availability-link" aria-label={`Edit ${member.name} availability`} onClick={()=>setEditing(member)} disabled={busy}><CalendarDays size={13}/>{new Set(member.availability.map(a=>a.dayOfWeek)).size} days available · Edit availability</button>
            </div>)}</div>
            <Pagination {...people} label="people"/><p className="input-hint">Select up to 50 people for this meeting.</p>
            <Button className="search-button" onClick={()=>void search()} disabled={busy || !group.length || !rooms.length}>{busy?<LoaderCircle size={18} className="spin"/>:<Search size={18}/>}Find Meeting Options<ArrowRight size={17}/></Button>
            {!group.length && members.length>0 && <p className="input-hint">Select at least one team member.</p>}
            {!rooms.length && <Button variant="outline" className="full-width add-first-room" onClick={()=>setRoomEditor("new")}><Plus size={15}/>Add your first room</Button>}
          </section>
          <section className="demo-panel"><div className="demo-panel-label"><DoorOpen size={16}/>YOUR WORKSPACE</div><h3>A place for every plan.</h3><p>Add rooms, change their details, and manage your bookings.</p><Button asChild variant="outline" className="full-width"><Link href="/app/rooms">Manage rooms<ArrowRight size={16}/></Link></Button><Link className="reset-link" href="/app/presets">Save time with meeting presets</Link></section>
        </aside><div className="results-column">
          {!members.length ? <section className="panel empty-state"><Users size={32}/><h2>Start with your people</h2><p>No fixed names or team size. Add anyone, choose who is joining, and set their availability.</p><Button onClick={()=>setMemberEditor("new")}><Plus size={16}/>Add first member</Button></section> : <Timeline members={group} day={day} setDay={setDay} common={result?.commonIntervals || []} selected={active} stale={stale}/>}
          {!rooms.length ? <section className="panel empty-state"><DoorOpen size={28}/><h3>Add a space to meet</h3><p>Give your room a name, location, opening hours, and seat count.</p><Button variant="outline" onClick={()=>setRoomEditor("new")}>Add room</Button></section> : <Results result={result} selected={selected} onSelect={select} onBook={setBooking} duration={submitted.duration} capacity={submitted.capacity} busy={busy} stale={stale}/>}
        </div></div>}
      <footer className="footer"><span>Your people. Your spaces. One shared plan.</span><span>MeetGrid · Weekly scheduling</span></footer>
    </main>
    {toast && <div className="toast" role="status"><span className="toast-icon"><Check size={15}/></span>{toast}<button className="icon-button small" aria-label="Dismiss update" onClick={()=>setToast("")}><X size={15}/></button></div>}
    {editing && <AvailabilityEditor member={editing} onClose={()=>setEditing(null)} onSave={saveAvailability}/>}
    {memberEditor && <MemberEditor member={memberEditor==="new"?undefined:memberEditor} onClose={()=>setMemberEditor(null)} onSave={async input=>{const saved=await api.saveMember(input,memberEditor==="new"?undefined:memberEditor.id);await refresh("Member saved. Set their availability to include them in matching.",memberEditor==="new"&&participantIds.length<50?[...participantIds,saved.id]:participantIds);}}/>}
    {roomEditor && <RoomEditor room={roomEditor==="new"?undefined:roomEditor} onClose={()=>setRoomEditor(null)} onSave={async input=>{await api.saveRoom(input,roomEditor==="new"?undefined:roomEditor.id);await refresh("Room saved.");}}/>}
    {booking && <BookingDialog rooms={rooms} initial={booking} onClose={()=>setBooking(null)} onSave={addBooking}/>}
    {confirmation && <ConfirmAction {...confirmation} onClose={()=>setConfirmation(null)}/>}
    {info && <Modal title="Your team. Your schedule." subtitle="Everything starts with the people and spaces you add." onClose={()=>setInfo(false)}><div className="how-it-works"><div><span>01</span><div><h3>Make it yours</h3><p>Add, rename, or remove team members. Set their weekly availability and choose who attends each search.</p></div></div><div><span>02</span><div><h3>Add your spaces</h3><p>Create rooms with any name, location, capacity, and opening hours. Existing bookings are checked automatically.</p></div></div><div><span>03</span><div><h3>Find and book</h3><p>Search for shared free time, book a matching room, or cancel a reservation from the room directory. Times repeat weekly.</p></div></div></div><Button className="full-width" onClick={()=>setInfo(false)}>Got it</Button></Modal>}
  </div>;
}
