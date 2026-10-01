'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Download, ArrowUpRight, RefreshCw } from 'lucide-react';
import { motion } from 'motion/react';
import { request } from '@/lib/api';
import { dayName, type Day } from '@/lib/types';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Pagination, usePagination } from '@/components/pagination';
import { FeatureGate } from './entitlements';

type RoomSummary = { id: string; name: string; location: string; capacity: number; bookings: number; reservedMinutes: number; openingMinutes: number; utilization: number };
type RoomDay = { roomId: string; roomName: string; day: Day; bookings: number; reservedMinutes: number; utilization: number; longestFreeMinutes: number; freeFrom: string; freeUntil: string };
type Report = { generatedAt: string; rooms: RoomSummary[]; days: RoomDay[]; reservedMinutes: number; openingMinutes: number; busiestDay: Day | '' };
const days: Day[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const hours = (minutes: number) => `${Math.round(minutes / 6) / 10} h`;

export function WorkspaceInsights({ operations = false }: { operations?: boolean }) {
  return <FeatureGate feature={operations ? 'operations_reports' : 'insights'} title={operations ? 'Give every space a better working week.' : 'See where your rooms work hardest.'} description={operations ? 'Compare demand by weekday, find the longest free slots, and export an operations report for your team.' : 'See reserved hours, utilization, and the rooms that need attention.'}><ReportContent operations={operations}/></FeatureGate>;
}
function ReportContent({ operations }: { operations: boolean }) {
  const [report, setReport] = useState<Report | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [query, setQuery] = useState('');
  const [day, setDay] = useState('');
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let active = true; setBusy(true); setError('');
    request<Report>(operations ? '/tools/operations' : '/tools/insights').then(data => { if (active) setReport(data); }).catch(e => { if (active) { setReport(null); setError(e.message); } }).finally(() => { if (active) setBusy(false); });
    return () => { active = false; };
  }, [operations, revision]);
  const rooms = report?.rooms.filter(room => `${room.name} ${room.location}`.toLowerCase().includes(query.toLowerCase())) ?? [];
  const roomIds = new Set(rooms.map(room => room.id));
  const rows = report?.days.filter(row => (!day || row.day === day) && roomIds.has(row.roomId)) ?? [];
  const roomPage = usePagination(rooms, 8, query);
  const dayPage = usePagination(rows, 14, `${query}:${day}`);
  function exportReport() {
    const values: (string | number)[][] = [['Room', 'Day', 'Reservations', 'Reserved minutes', 'Utilization percent', 'Longest free slot minutes', 'Free from', 'Free until'], ...rows.map(row => [row.roomName, row.day, row.bookings, row.reservedMinutes, row.utilization, row.longestFreeMinutes, row.freeFrom, row.freeUntil])];
    const contents = '\uFEFF' + values.map(row => row.map(value => { let text = String(value); if (/^\s*[=+@-]/.test(text)) text = `'${text}`; return `"${text.replaceAll('"', '""')}"`; }).join(',')).join('\r\n');
    const url = URL.createObjectURL(new Blob([contents], { type: 'text/csv;charset=utf-8' })); const link = document.createElement('a'); link.href = url; link.download = 'meetgrid-weekly-operations.csv'; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1500);
  }
  return <>
    <div className="page-heading report-heading"><div><span className="kicker">{operations ? 'COLLECTIVE · A CLEARER WEEK' : 'STUDIO · SEE THE SHAPE OF YOUR WEEK'}</span><h1>{operations ? 'Every room. Every weekday.' : 'Make space work smarter.'}</h1><p>Based on your saved weekly reservations and room opening hours.</p></div><Button variant="outline" disabled={busy} onClick={() => setRevision(n => n + 1)}><RefreshCw size={15}/>Refresh report</Button></div>
    {error && <div className="feature-upgrade"><p role="alert" className="form-error">{error}</p><Button asChild><Link href="/app/billing">View plan access</Link></Button></div>}
    {busy && !report && <p role="status">Preparing your workspace report…</p>}
    {report && <>
      <div className="report-metrics"><Card><CardContent><span>Reserved each week</span><strong>{hours(report.reservedMinutes)}</strong></CardContent></Card><Card><CardContent><span>Available opening time</span><strong>{hours(report.openingMinutes)}</strong></CardContent></Card><Card><CardContent><span>Overall utilization</span><strong>{report.openingMinutes ? Math.round(report.reservedMinutes / report.openingMinutes * 100) : 0}%</strong></CardContent></Card><Card><CardContent><span>Busiest weekday</span><strong>{report.busiestDay ? dayName(report.busiestDay) : 'No bookings yet'}</strong></CardContent></Card></div>
      {!report.rooms.length ? <section className="feature-upgrade"><h2>Your spaces will tell the story.</h2><p>Add rooms and weekly reservations to see the report.</p><Button asChild><Link href="/app/rooms">Add your spaces<ArrowUpRight size={16}/></Link></Button></section> : <>
        <div className="report-filters"><label>Find a room<Input placeholder="Room name or location" value={query} onChange={e => setQuery(e.target.value)}/></label>{operations && <label>Weekday<select value={day} onChange={e => setDay(e.target.value)}><option value="">Every weekday</option>{days.map(d => <option key={d} value={d}>{dayName(d)}</option>)}</select></label>}{operations ? <Button variant="outline" disabled={!rows.length || busy} onClick={exportReport}><Download size={16}/>Export {query || day ? 'filtered' : 'full'} report</Button> : <Button asChild variant="outline"><Link href="/app/operations">Operations reports · Collective<ArrowUpRight size={16}/></Link></Button>}</div>
        <section className="data-panel"><div className="section-top"><h2>Room utilization</h2><span>Most used first</span></div>{roomPage.items.map(room => <div className="utilization-row" key={room.id}><div><strong>{room.name}</strong><span>{hours(room.reservedMinutes)} reserved · {room.bookings} reservations · {room.utilization}%</span></div><div className="utilization-track"><motion.div initial={false} animate={{ width: `${Math.min(room.utilization, 100)}%` }} transition={{ type: 'spring', stiffness: 120, damping: 25 }}/></div></div>)}{!rooms.length && <p>No rooms match this search.</p>}<Pagination {...roomPage} label="rooms"/></section>
        {operations && <><section className="data-panel report-heatmap"><h2>A week at a glance</h2><p>Each cell shows the share of that room’s daily opening time already reserved.</p><div className="report-scroll"><table><thead><tr><th>Room</th>{days.map(d => <th key={d}>{dayName(d).slice(0, 3)}</th>)}</tr></thead><tbody>{roomPage.items.map(room => <tr key={room.id}><th>{room.name}</th>{days.map(d => { const item = report.days.find(row => row.roomId === room.id && row.day === d); return <td key={d} title={`${room.name}, ${dayName(d)}: ${item?.utilization ?? 0}% reserved`}><span style={{ backgroundColor: `rgba(103,86,167,${0.08 + Math.min(item?.utilization ?? 0, 100) / 100 * 0.25})` }}>{item?.utilization ?? 0}%</span></td>; })}</tr>)}</tbody></table></div></section>
          <section className="data-panel"><div className="section-top"><h2>Find the space in your schedule</h2><span>{rows.length} room / weekday entries</span></div><div className="report-scroll"><table className="report-detail-table"><thead><tr><th>Room / day</th><th>Reserved</th><th>Largest free slot</th><th>Free window</th></tr></thead><tbody>{dayPage.items.map(row => <tr key={`${row.roomId}-${row.day}`}><th>{row.roomName}<small>{dayName(row.day)}</small></th><td>{hours(row.reservedMinutes)}</td><td>{row.longestFreeMinutes ? hours(row.longestFreeMinutes) : 'Fully reserved'}</td><td>{row.longestFreeMinutes ? `${row.freeFrom}–${row.freeUntil}` : '—'}</td></tr>)}</tbody></table></div><Pagination {...dayPage} label="room days"/>{!rows.length && <p>No report rows match these filters.</p>}</section></>}
      </>}
      <p className="insight-note">Rooms use their configured daily opening hours across all seven days. Reserved time is counted once when intervals overlap. These reports describe scheduled time, not attendance. Updated {new Date(report.generatedAt).toLocaleString()}.</p>
    </>}
  </>;
}
