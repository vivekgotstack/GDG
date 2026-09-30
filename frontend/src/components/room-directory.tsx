"use client";
import {useState} from 'react';
import Link from 'next/link';
import {Pagination,usePagination} from './pagination';
import {Input} from './ui/input';
import { Clock3, DoorOpen, MapPin, Plus, Users, Pencil, Trash2, X } from "lucide-react";
import { dayName, range, type Booking, type Room } from "@/lib/types";
import { Button } from "./ui/button";
export function RoomDirectory({ rooms, bookings, onBook, onAdd, onEdit, onDelete, onCancel }: {
  rooms: Room[]; bookings: Booking[]; onBook: (roomId: string) => void; onAdd:()=>void;
  onEdit:(room:Room)=>void; onDelete:(room:Room)=>void; onCancel:(booking:Booking)=>void;
}) {
  const [query,setQuery]=useState("");const page=usePagination(rooms.filter(r=>`${r.name} ${r.location}`.toLowerCase().includes(query.toLowerCase())),6,query);
  return <section className="directory panel"><div className="section-top"><div><h2>Your rooms <span className="count-badge">{rooms.length}</span></h2><p>Manage spaces, opening hours, and weekly reservations.</p></div><Button onClick={onAdd}><Plus size={16}/>Add room</Button></div>
    {!rooms.length && <div className="empty-state"><DoorOpen size={28}/><h3>Make space for your team</h3><p>Add your first room with its capacity, location, and opening hours.</p></div>}
    <Input aria-label="Search rooms" placeholder="Find a room or location…" value={query} onChange={e=>setQuery(e.target.value)}/><div className="directory-grid">{page.items.map(room => <article key={room.id} data-room={room.id} className="directory-room">
      <div className="directory-room-top"><span className="directory-icon"><DoorOpen size={26}/></span><span className="seats-badge"><Users size={13}/>{room.capacity} seats</span></div>
      <h3>{room.name}</h3><p className="directory-location"><MapPin size={13}/>{room.location}</p><p className="directory-hours"><Clock3 size={14}/>{range(room.openTime,room.closeTime)}</p>
      <div className="management-actions"><Button variant="outline" size="sm" onClick={()=>onEdit(room)} aria-label={`Edit ${room.name}`}><Pencil size={14}/>Edit room</Button><Button variant="ghost" size="sm" onClick={()=>onDelete(room)} aria-label={`Delete ${room.name}`}><Trash2 size={14}/>Delete</Button></div>
      <div className="existing-bookings"><span className="small-label">WEEKLY BOOKINGS</span>{bookings.filter(b => b.roomId === room.id).length ? bookings.filter(b => b.roomId === room.id).slice(0,6).map(b => <div key={b.id}><strong>{dayName(b.dayOfWeek).slice(0,3)}</strong><span>{range(b.startTime,b.endTime)}</span><button className="icon-button small danger" aria-label={`Cancel ${room.name} booking on ${dayName(b.dayOfWeek)} at ${range(b.startTime,b.endTime)}`} onClick={()=>onCancel(b)}><X size={14}/></button></div>) : <p className="muted">No reservations yet.</p>}</div>
      {bookings.filter(b=>b.roomId===room.id).length>6&&<Link href="/app/bookings">View all reservations →</Link>}<Button variant="outline" className="full-width" onClick={() => onBook(room.id)}><Plus size={15}/>Book room</Button>
    </article>)}</div>
  <Pagination {...page} label="rooms"/></section>;
}
