"use client";
import { useState } from "react";
import { FlaskConical, LoaderCircle } from "lucide-react";
import { Modal } from "./modal";
import { DAYS, dayName, range, type BookingInput, type Day, type Room } from "@/lib/types";
export function BookingDialog({ rooms, initial, onClose, onSave }: {
  rooms: Room[]; initial?: Partial<BookingInput>; onClose: () => void; onSave: (input: BookingInput) => Promise<void>;
}) {
  const [roomId, setRoomId] = useState(initial?.roomId || "lab-2");
  const [dayOfWeek, setDay] = useState<Day>(initial?.dayOfWeek || "TUESDAY");
  const [startTime, setStart] = useState(initial?.startTime?.slice(0,5) || "14:00");
  const [endTime, setEnd] = useState(initial?.endTime?.slice(0,5) || "15:00");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const room = rooms.find(r => r.id === roomId);
  return <Modal title="Give the schedule a plot twist." subtitle="Add a room booking and watch your meeting options adapt." onClose={onClose} busy={busy}>
    <form onSubmit={async e => {
      e.preventDefault(); setError("");
      if (startTime >= endTime) { setError("The end time must be later than the start time."); return; }
      setBusy(true);
      try { await onSave({roomId,dayOfWeek,startTime,endTime}); onClose(); }
      catch (e) { setError(e instanceof Error ? e.message : "Could not add booking."); }
      finally { setBusy(false); }
    }}>
      <fieldset disabled={busy} className="booking-fields">
        <label>Room<select value={roomId} onChange={e => setRoomId(e.target.value)}>{rooms.map(r => <option key={r.id} value={r.id}>{r.name} · {r.capacity} seats</option>)}</select></label>
        {room && <span className="muted room-hours">Open {range(room.openTime,room.closeTime)}</span>}
        <label>Day<select value={dayOfWeek} onChange={e => setDay(e.target.value as Day)}>{DAYS.map(day => <option key={day} value={day}>{dayName(day)}</option>)}</select></label>
        <div className="two-fields"><label>From<input type="time" value={startTime} onChange={e => setStart(e.target.value)} step={60} required/></label><label>Until<input type="time" value={endTime} onChange={e => setEnd(e.target.value)} step={60} required/></label></div>
      </fieldset>
      <div className="demo-tip"><FlaskConical size={18}/><p>This books a room in your demo workspace. Any affected options will be recalculated automatically.</p></div>
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="modal-actions"><button className="button secondary" type="button" onClick={onClose} disabled={busy}>Cancel</button><button className="button primary" disabled={busy}>{busy && <LoaderCircle className="spin" size={16}/>}Add booking & update</button></div>
    </form>
  </Modal>;
}
