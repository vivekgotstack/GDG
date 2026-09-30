"use client";
import { useState } from "react";
import { FlaskConical, LoaderCircle } from "lucide-react";
import { Modal } from "./modal";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Textarea } from "./ui/textarea";
import { DAYS, dayName, range, type BookingInput, type Day, type Room } from "@/lib/types";
export function BookingDialog({ rooms, initial, onClose, onSave,editing=false }: {
  rooms: Room[]; initial?: Partial<BookingInput>; onClose: () => void; onSave: (input: BookingInput) => Promise<void>;editing?:boolean;
}) {
  const defaultRoom = rooms.find(r => r.id === initial?.roomId) || rooms[0];
  const [roomId, setRoomId] = useState(defaultRoom?.id || "");
  const [dayOfWeek, setDay] = useState<Day>(initial?.dayOfWeek || "MONDAY");
  const [startTime, setStart] = useState(initial?.startTime?.slice(0,5) || defaultRoom?.openTime.slice(0,5) || "09:00");
  const [endTime, setEnd] = useState(initial?.endTime?.slice(0,5) || defaultRoom?.closeTime.slice(0,5) || "10:00");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [title,setTitle]=useState(initial?.title||'Team meeting');
  const [notes,setNotes]=useState(initial?.notes||'');
  const room = rooms.find(r => r.id === roomId);
  return <Modal title={editing?'Edit your reservation':'Book your room'} subtitle="Set the purpose, room, and weekly time. Conflicts are checked before saving." onClose={onClose} busy={busy}>
    <form onSubmit={async e => {
      e.preventDefault(); setError("");
      if (startTime >= endTime) { setError("The end time must be later than the start time."); return; }
      if (room && (startTime < room.openTime.slice(0,5) || endTime > room.closeTime.slice(0,5))) {
        setError(`${room.name} is open ${range(room.openTime,room.closeTime)}. Choose a time within those hours.`); return;
      }
      setBusy(true);
      try { await onSave({roomId,dayOfWeek,startTime,endTime,title,notes}); onClose(); }
      catch (e) { setError(e instanceof Error ? e.message : "Could not add booking."); }
      finally { setBusy(false); }
    }}>
      <fieldset disabled={busy} className="booking-fields">
        <label>Meeting title<Input value={title} onChange={e=>setTitle(e.target.value)} maxLength={120} required placeholder="What are you getting together for?"/></label>
        <label>Room<select value={roomId} onChange={e => setRoomId(e.target.value)}>{rooms.map(r => <option key={r.id} value={r.id}>{r.name} · {r.capacity} seats</option>)}</select></label>
        {room && <span className="muted room-hours">Open {range(room.openTime,room.closeTime)}</span>}
        <label>Day<select value={dayOfWeek} onChange={e => setDay(e.target.value as Day)}>{DAYS.map(day => <option key={day} value={day}>{dayName(day)}</option>)}</select></label>
        <div className="two-fields"><label>From<input type="time" value={startTime} onChange={e => setStart(e.target.value)} step={60} required/></label><label>Until<input type="time" value={endTime} onChange={e => setEnd(e.target.value)} step={60} required/></label></div>
        <label>Agenda or notes <span className="muted">(optional)</span><Textarea value={notes} onChange={e=>setNotes(e.target.value)} maxLength={2000} rows={3} placeholder="Add the purpose or points to cover."/></label>
      </fieldset>
      <div className="demo-tip"><FlaskConical size={18}/><p>This reservation repeats weekly. Changes apply to the whole weekly series.</p></div>
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="modal-actions"><Button variant="outline" type="button" onClick={onClose} disabled={busy}>Cancel</Button><Button type="submit" disabled={busy||!roomId}>{busy && <LoaderCircle className="spin" size={16}/>} {editing?'Save reservation':'Add booking & update'}</Button></div>
    </form>
  </Modal>;
}
