"use client";
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { motion,MotionConfig } from 'motion/react';
import { LayoutDashboard,CalendarDays,DoorOpen,BookOpen,BarChart3,Settings,ArrowUpRight,Menu,X,CreditCard } from 'lucide-react';
import { useState } from 'react';
import { Brand } from './brand';
import { useSession } from './session';
import { AuthForm } from './auth-form';
const navigation=[['/app','Overview',LayoutDashboard],['/app/planner','Meeting planner',CalendarDays],['/app/rooms','Spaces & rooms',DoorOpen],['/app/bookings','Bookings',BookOpen],['/app/presets','Meeting presets',BookOpen],['/app/insights','Insights',BarChart3],['/app/billing','Plans & billing',CreditCard],['/app/settings','Settings',Settings]] as const;
export function WorkspaceShell({children}:{children:React.ReactNode}){
 const path=usePathname();const {user,loading}=useSession();const [open,setOpen]=useState(false);
 if(loading)return <div className="workspace-loading"><span className="brand-mark"><i/><i/><i/><i/></span><p>Opening your workspace…</p></div>;
 if(!user)return <AuthForm inline/>;
 return <MotionConfig reducedMotion="user"><div className="product-workspace"><aside className={`product-sidebar ${open?'nav-open':''}`}><Brand/><button className="mobile-menu" onClick={()=>setOpen(false)} aria-label="Close navigation"><X size={20}/></button><div className="workspace-label"><span>{user.workspaceName[0]?.toUpperCase()}</span><div><strong>{user.workspaceName}</strong><small>{user.plan} workspace</small></div></div><span className="nav-eyebrow">YOUR EVERYDAY, ORGANIZED</span><nav aria-label="Workspace navigation">{navigation.map(([href,label,Icon])=><Link href={href} key={href} onClick={()=>setOpen(false)} className={path===href?'current':''}>{path===href&&<motion.span className="nav-active" layoutId="workspace-nav" transition={{type:'spring',stiffness:400,damping:35}}/>}<Icon size={18}/><span>{label}</span>{path===href&&<span className="nav-pip"/>}</Link>)}</nav><div className="sidebar-note"><span>✳</span><h3>Good things happen<br/>when we meet.</h3><p>Less time coordinating.<br/>More time creating.</p><Link href="/pricing">Explore plans<ArrowUpRight size={16}/></Link></div><div className="sidebar-user"><span>{user.name[0]?.toUpperCase()}</span><div><strong>{user.name}</strong><small>{user.email}</small></div></div></aside><div className="product-main"><header className="workspace-topbar"><button className="mobile-menu" aria-label="Open navigation" onClick={()=>setOpen(!open)}><Menu size={21}/></button><span>{navigation.find(([href])=>href===path)?.[1] || 'Workspace'}</span><span className="timezone-pill">{user.timezone.replaceAll('_',' ')}</span><Link href="/app/planner" className="quick-plan">Plan a meeting <ArrowUpRight size={15}/></Link></header><div className="product-content">{children}</div></div></div></MotionConfig>;
}
