"use client";
import {Pagination,usePagination} from './pagination';
import { ArrowUpRight, Check, ChevronDown, Clock3, DoorOpen, MapPin, Users, X, SearchX } from "lucide-react";
import { dayName, range, type BookingInput, type MeetingOption, type SearchResult } from "@/lib/types";
import { Button } from "./ui/button";
export function Results({ result, selected, onSelect, onBook, duration, capacity, busy, stale }: {
  result: SearchResult | null; selected: string; onSelect: (option: MeetingOption) => void; onBook: (input: BookingInput) => void; duration: number; capacity: number; busy: boolean; stale: boolean;
}) {
  const page=usePagination(result?.options||[],6,JSON.stringify([result?.options.map(o=>o.id),duration,capacity]));
  return <section className={`results-section ${busy ? "is-loading" : ""}`} aria-labelledby="results-heading" aria-busy={busy}>
    <div className="results-heading"><div><div className="heading-line"><h2 id="results-heading">A good time to get together</h2><span className="count-badge">{result?.options.length || 0}</span></div><p>{stale ? "Your settings changed. Run a search to update these options." : `Room for ${capacity}. ${duration} minutes. Everyone included.`}</p></div><span className="verified-label"><Check size={14}/>All constraints checked</span></div>
    {!result ? <div className="empty-state"><Clock3 size={26}/><h3>Your next meeting starts here</h3><p>Set your preferences and find a time and room that work for everyone.</p></div> : !result.options.length ? <div className="empty-state"><SearchX size={30}/><h3>No perfect overlap just yet</h3><p>{!result.commonIntervals.length ? "Your team has no shared free time. Add availability for a day that works for everyone." : result.blockedCandidates ? "Your team is free, but every matching room is busy, closed, or too small. Adjust the time, capacity, or demo bookings." : "The common free periods are too short for this meeting on a half-hour boundary. Try a shorter duration."}</p></div> :
    <div className="result-grid">{page.items.map((option, index) => <article key={option.id} className={`result-card ${selected === option.id ? "selected" : ""}`} data-testid="meeting-option">
      <button className="result-select" onClick={() => onSelect(option)} aria-label={`View ${dayName(option.dayOfWeek)} ${range(option.startTime,option.endTime)} on timeline`} aria-pressed={selected === option.id}>
        <div className="result-day"><span className="calendar-tile">{dayName(option.dayOfWeek).slice(0,3).toUpperCase()}<span><Clock3 size={17}/></span></span><div><span className="option-label">OPTION {String((page.page-1)*page.size+index + 1).padStart(2,"0")}</span><h3>{dayName(option.dayOfWeek)}</h3></div></div>
        <span className="result-arrow"><ArrowUpRight size={19}/></span>
        <div className="result-time">{range(option.startTime,option.endTime)}</div>
        <div className="result-meta"><span><Check size={13}/>{capacity} seats needed</span><span><Users size={13}/>Everyone is free</span></div>
      </button>
      <div className="room-matches"><div className="room-matches-title"><span><DoorOpen size={14}/>{option.availableRooms.length} suitable {option.availableRooms.length === 1 ? "room" : "rooms"}</span><span className="small-label">READY WHEN YOU ARE</span></div>
        {option.availableRooms.map(room => <div className="matched-room" data-room={room.id} key={room.id}><span className="room-icon"><DoorOpen size={18}/></span><div className="matched-room-text"><h4>{room.name}</h4><span><Users size={11}/>{room.capacity} seats<span className="meta-dot">·</span><MapPin size={11}/>{room.location.split(" · ")[0]}</span></div><Button variant="outline" size="sm" disabled={busy || stale} aria-label={`Book ${room.name} on ${dayName(option.dayOfWeek)} at ${range(option.startTime,option.endTime)}`} onClick={() => onBook({roomId:room.id,dayOfWeek:option.dayOfWeek,startTime:option.startTime,endTime:option.endTime})}>Book room</Button></div>)}
      </div>
      <details className="rejected-rooms"><summary>Why not the other {option.rejectedRooms.length} {option.rejectedRooms.length === 1 ? "room" : "rooms"}?<ChevronDown size={15}/></summary><div>{option.rejectedRooms.length ? option.rejectedRooms.map(({room,reasons}) => <div className="rejected-room" key={room.id}><span className="reject-icon"><X size={13}/></span><div><strong>{room.name}</strong>{reasons.map(reason => <p key={reason}>{reason}</p>)}</div></div>) : <p className="muted">Every room meets your constraints.</p>}</div></details>
    </article>)}</div>}
  <Pagination {...page} label="options"/></section>;
}
