"use client";
import { useState } from "react";
import { Plus, Trash2, LoaderCircle, CalendarDays } from "lucide-react";
import { Modal } from "./modal";
import { DAYS, dayName, type Availability, type Member } from "@/lib/types";
export function AvailabilityEditor({ member, onClose, onSave }: {
  member: Member; onClose: () => void; onSave: (availability: Availability[]) => Promise<void>;
}) {
  const [draft, setDraft] = useState<Availability[]>(member.availability.map(a => ({ ...a, startTime: a.startTime.slice(0,5), endTime: a.endTime.slice(0,5) })));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  function update(index: number, key: "startTime" | "endTime", value: string) {
    setDraft(old => old.map((item, i) => i === index ? { ...item, [key]: value } : item));
  }
  return <Modal title={`${member.name}’s availability`} subtitle="Add the times you’re free in a typical week." onClose={onClose} busy={busy}>
    <form onSubmit={async e => {
      e.preventDefault(); setError("");
      if (draft.some(a => !a.startTime || !a.endTime || a.startTime >= a.endTime)) { setError("Each end time must be later than its start time."); return; }
      setBusy(true);
      try { await onSave(draft); onClose(); } catch (e) { setError(e instanceof Error ? e.message : "Could not save availability."); } finally { setBusy(false); }
    }}>
      <fieldset disabled={busy} className="editor-days">
        {DAYS.map(day => <div className="editor-day" key={day}>
          <div className="editor-day-label"><span>{dayName(day)}</span><button type="button" className="icon-button small" aria-label={`Add ${dayName(day)} range`} onClick={() => setDraft([...draft, { dayOfWeek: day, startTime: "09:00", endTime: "17:00" }])}><Plus size={16}/></button></div>
          <div className="editor-ranges">
            {!draft.some(a => a.dayOfWeek === day) && <span className="muted no-ranges">No availability</span>}
            {draft.map((a,i) => a.dayOfWeek === day && <div className="time-range" key={i}>
              <input type="time" aria-label={`${dayName(day)} start ${i + 1}`} value={a.startTime} required step={60} onChange={e => update(i,"startTime",e.target.value)}/>
              <span className="muted">to</span>
              <input type="time" aria-label={`${dayName(day)} end ${i + 1}`} value={a.endTime} required step={60} onChange={e => update(i,"endTime",e.target.value)}/>
              <button type="button" className="icon-button small danger" aria-label={`Remove ${dayName(day)} range ${i+1}`} onClick={() => setDraft(draft.filter((_,j) => i !== j))}><Trash2 size={15}/></button>
            </div>)}
          </div>
        </div>)}
      </fieldset>
      <p className="form-note"><CalendarDays size={15}/> Times repeat weekly. Overlapping ranges are combined.</p>
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="modal-actions"><button type="button" className="button secondary" onClick={onClose} disabled={busy}>Cancel</button><button className="button primary" disabled={busy}>{busy && <LoaderCircle className="spin" size={16}/>}Save availability</button></div>
    </form>
  </Modal>;
}
