import { Clock3, DoorOpen, MapPin, Plus, Users } from "lucide-react";
import { dayName, range, type Booking, type Room } from "@/lib/types";
export function RoomDirectory({ rooms, bookings, onBook }: { rooms: Room[]; bookings: Booking[]; onBook: (roomId: string) => void }) {
  return <section className="directory panel"><div className="section-top"><div><h2>Five rooms. Plenty of possibilities.</h2><p>A little space for the next big idea. Check hours and existing bookings below.</p></div><span className="count-badge">{rooms.length}</span></div>
    <div className="directory-grid">{rooms.map(room => <article key={room.id} className="directory-room">
      <div className="directory-room-top"><span className="directory-icon"><DoorOpen size={26}/></span><span className="seats-badge"><Users size={13}/>{room.capacity} seats</span></div>
      <h3>{room.name}</h3><p className="directory-location"><MapPin size={13}/>{room.location}</p><p className="directory-hours"><Clock3 size={14}/>{range(room.openTime,room.closeTime)}</p>
      <div className="existing-bookings"><span className="small-label">EXISTING BOOKINGS</span>{bookings.filter(b => b.roomId === room.id).length ? bookings.filter(b => b.roomId === room.id).map(b => <div key={b.id}><strong>{dayName(b.dayOfWeek).slice(0,3)}</strong><span>{range(b.startTime,b.endTime)}</span>{b.source === "demo" && <i title="Added in this demo" className="demo-dot"/>}</div>) : <p className="muted">A clear week. Make it yours.</p>}</div>
      <button className="button secondary full-width" onClick={() => onBook(room.id)}><Plus size={15}/>Add demo booking</button>
    </article>)}</div>
  </section>;
}
