"use client";
import { useState } from "react";
import { LoaderCircle } from "lucide-react";
import { Modal } from "./modal";
import { Button } from "./ui/button";
import type { Member, MemberInput, Room, RoomInput } from "@/lib/types";

export function MemberEditor({ member, onSave, onClose }: {
  member?: Member; onSave: (input: MemberInput) => Promise<void>; onClose: () => void;
}) {
  const [name, setName] = useState(member?.name || "");
  const [color, setColor] = useState(member?.color || "lavender");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  return <Modal title={member ? "Edit team member" : "Add a team member"} subtitle="Use any name you like. Availability is managed separately." onClose={onClose} busy={busy}>
    <form onSubmit={async e => { e.preventDefault(); if (!name.trim()) { setError("Enter a name."); return; } setBusy(true); setError(""); try { await onSave({name:name.trim(),color}); onClose(); } catch(e) { setError(e instanceof Error ? e.message : "Could not save member."); } finally { setBusy(false); } }}>
      <fieldset disabled={busy} className="booking-fields">
        <label>Name<input required maxLength={80} value={name} onChange={e => setName(e.target.value)} placeholder="Team member name" autoComplete="off"/></label>
        <label>Color<select value={color} onChange={e => setColor(e.target.value)}><option value="lavender">Purple</option><option value="pink">Pink</option><option value="apricot">Yellow</option><option value="blue">Blue</option><option value="sage">Green</option></select></label>
      </fieldset>
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="modal-actions"><Button type="button" variant="outline" disabled={busy} onClick={onClose}>Cancel</Button><Button type="submit" disabled={busy}>{busy && <LoaderCircle size={16} className="spin"/>}{member ? "Save member" : "Add member"}</Button></div>
    </form>
  </Modal>;
}

export function RoomEditor({ room, onSave, onClose }: {
  room?: Room; onSave: (input: RoomInput) => Promise<void>; onClose: () => void;
}) {
  const [draft, setDraft] = useState<RoomInput>({name:room?.name || "",location:room?.location || "",capacity:room?.capacity || 4,openTime:room?.openTime.slice(0,5) || "09:00",closeTime:room?.closeTime.slice(0,5) || "18:00"});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  return <Modal title={room ? "Edit room" : "Add a room"} subtitle="Set the name, location, seats, and weekly opening hours." onClose={onClose} busy={busy}>
    <form onSubmit={async e => { e.preventDefault(); setError(""); if (!draft.name.trim() || !draft.location.trim()) { setError("Enter a room name and location."); return; } if (draft.openTime >= draft.closeTime) { setError("Closing time must be later than opening time."); return; } setBusy(true); try { await onSave({...draft,name:draft.name.trim(),location:draft.location.trim()}); onClose(); } catch(e) { setError(e instanceof Error ? e.message : "Could not save room."); } finally { setBusy(false); } }}>
      <fieldset disabled={busy} className="booking-fields">
        <label>Room name<input required maxLength={100} value={draft.name} onChange={e => setDraft({...draft,name:e.target.value})} placeholder="Room name"/></label>
        <label>Location<input required maxLength={100} value={draft.location} onChange={e => setDraft({...draft,location:e.target.value})} placeholder="Building, floor, or address"/></label>
        <label>Seats<input required type="number" min={1} max={1000} value={draft.capacity || ""} onChange={e => setDraft({...draft,capacity:Number(e.target.value)})}/></label>
        <div className="two-fields"><label>Opens<input required type="time" step={60} value={draft.openTime} onChange={e => setDraft({...draft,openTime:e.target.value})}/></label><label>Closes<input required type="time" step={60} value={draft.closeTime} onChange={e => setDraft({...draft,closeTime:e.target.value})}/></label></div>
      </fieldset>
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="modal-actions"><Button type="button" variant="outline" disabled={busy} onClick={onClose}>Cancel</Button><Button type="submit" disabled={busy}>{busy && <LoaderCircle size={16} className="spin"/>}{room ? "Save room" : "Add room"}</Button></div>
    </form>
  </Modal>;
}

export function ConfirmAction({title, description, label, action, onClose}: {title:string; description:string; label:string; action:()=>Promise<void>; onClose:()=>void}) {
  const [busy,setBusy] = useState(false);
  const [error,setError] = useState("");
  return <Modal title={title} subtitle={description} busy={busy} onClose={onClose}>
    {error && <p role="alert" className="form-error">{error}</p>}
    <div className="modal-actions"><Button variant="outline" disabled={busy} onClick={onClose}>Keep it</Button><Button variant="destructive" disabled={busy} onClick={async()=>{setBusy(true);setError("");try{await action();onClose();}catch(e){setError(e instanceof Error ? e.message : "Could not complete action.");}finally{setBusy(false);}}}>{busy && <LoaderCircle size={16} className="spin"/>}{label}</Button></div>
  </Modal>;
}
