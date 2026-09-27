import { Check, Clock3, Info, CalendarDays } from "lucide-react";
import { DAYS, dayName, minutes, range, type Availability, type Day, type Member, type MeetingOption } from "@/lib/types";
export function Timeline({ members, day, setDay, common, selected, stale }: {
  members: Member[]; day: Day; setDay: (day: Day) => void; common: Availability[]; selected?: MeetingOption; stale: boolean;
}) {
  const ranges = members.flatMap(m => m.availability.filter(a => a.dayOfWeek === day));
  const low = Math.min(480, ...ranges.map(a => Math.floor(minutes(a.startTime) / 60) * 60));
  const high = Math.max(1080, ...ranges.map(a => Math.ceil(minutes(a.endTime) / 60) * 60));
  const span = high - low;
  const position = (a: Availability) => ({ left: `${(minutes(a.startTime) - low) / span * 100}%`, width: `${(minutes(a.endTime) - minutes(a.startTime)) / span * 100}%` });
  const intervals = common.filter(a => a.dayOfWeek === day);
  const highlight = selected?.dayOfWeek === day ? selected : undefined;
  const ticks = Array.from({ length: 6 }, (_, i) => low + span * i / 5);
  return <section className="panel timeline-panel" aria-labelledby="timeline-heading">
    <div className="section-top"><div className="title-with-icon"><span className="soft-icon"><CalendarDays size={18}/></span><div><h2 id="timeline-heading">Your week, at a glance</h2><p>Different schedules. A little common ground.</p></div></div><span className="small-label desktop-label">WEEKLY AVAILABILITY</span></div>
    <div className="week-tabs" aria-label="Timeline day">{DAYS.map(d => <button key={d} onClick={() => setDay(d)} aria-pressed={day === d} className={day === d ? "active" : ""}><span className="day-full">{dayName(d)}</span><span className="day-short">{dayName(d).slice(0,3)}</span><span className={common.some(a => a.dayOfWeek === d) ? "day-dot matched" : "day-dot"}/></button>)}</div>
    <div className="timeline-scroll"><div className="timeline-chart">
      <div className="timeline-hours"><span/><div>{ticks.map((minute, i) => <span key={i} style={{ left: `${i * 20}%` }}>{Math.floor(minute/60) % 12 || 12}{minute >= 720 ? "pm" : "am"}</span>)}</div></div>
      {members.map(member => <div className="timeline-row" key={member.id}>
        <div className="timeline-name"><span className={`avatar tiny ${member.color}`}>{member.name[0]}</span>{member.name}</div>
        <div className="timeline-track">
          {highlight && <div className="slot-highlight" style={position(highlight)}/>}
          {member.availability.filter(a => a.dayOfWeek === day).map((a,i) => <div key={i} title={`${member.name}: ${range(a.startTime,a.endTime)}`} aria-label={`${member.name} available ${range(a.startTime,a.endTime)}`} className={`availability-bar ${member.color}`} style={position(a)}><span>{range(a.startTime,a.endTime).replaceAll(":00","")}</span></div>)}
        </div>
      </div>)}
      <div className="timeline-row common-row"><div className="timeline-name"><span className="common-check"><Check size={12}/></span>Everyone</div><div className="timeline-track">
        {intervals.map((a,i) => <div key={i} className="common-bar" title={`Everyone is free: ${range(a.startTime,a.endTime)}`} aria-label={`Common availability ${range(a.startTime,a.endTime)}`} style={position(a)}><Check size={12}/><span>Free</span></div>)}
        {!intervals.length && <span className="no-overlap">No shared availability this day</span>}
      </div></div>
    </div></div>
    <div className="timeline-footer"><span><i className="legend-swatch"/>Individual availability</span><span><i className="legend-swatch shared"/>Everyone is free</span><span className="timeline-hint"><Info size={13}/>{stale ? "Update your search to apply changes" : "Campus local time"}</span></div>
    <div className="overlap-note"><Clock3 size={16}/>{intervals.length ? <span>Common ground on {dayName(day)}: <strong>{intervals.map(a => range(a.startTime,a.endTime)).join(", ")}</strong></span> : <span>No common ground on {dayName(day)}. Try another day or edit the team’s availability.</span>}</div>
  </section>;
}
