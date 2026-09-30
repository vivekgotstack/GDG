import { notFound } from 'next/navigation';
import MeetGrid from '@/components/meetgrid';
import { Dashboard } from '@/components/product/dashboard';
import { Presets } from '@/components/product/presets';
import { Settings } from '@/components/product/settings';
import { AdminPanel } from '@/components/product/admin-panel';
import { WorkspaceTools } from '@/components/product/workspace-tools';
import { Pricing } from '@/components/product/pricing';
export default async function Section({params,searchParams}:{params:Promise<{section:string}>;searchParams:Promise<{duration?:string;capacity?:string}>}){
 const {section}=await params;const query=await searchParams;
 if(section==='planner'||section==='rooms')return <><div className="page-heading"><div><span className="kicker">{section==='planner'?'FROM BACK-AND-FORTH TO BOOKED':'A PLACE FOR EVERY GOOD IDEA'}</span><h1>{section==='planner'?'Find your common ground.':'Your spaces, your way.'}</h1><p>{section==='planner'?'Choose the people. Set the time you need. We’ll find the overlap.':'Add and customize your rooms, capacity, opening hours, and bookings.'}</p></div></div><MeetGrid view={section} initialDuration={Math.min(480,Math.max(15,Number(query.duration)||60))} initialCapacity={Math.min(1000,Math.max(1,Number(query.capacity)||1))}/></>;
 if(section==='bookings'||section==='insights')return <Dashboard mode={section}/>;
 if(['admin','admin-content','admin-plans','admin-users','admin-audit'].includes(section))return <AdminPanel section={section}/>;
 if(['tools','import','export'].includes(section))return <WorkspaceTools mode={section==='tools'?'index':section as 'import'|'export'}/>;
 if(section==='presets')return <Presets/>;
 if(section==='settings')return <Settings/>;
 if(section==='billing')return <Pricing embedded/>;
 notFound();
}
