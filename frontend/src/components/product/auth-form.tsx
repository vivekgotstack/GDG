"use client";
import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowRight,Eye,EyeOff,LoaderCircle } from 'lucide-react';
import { request } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useSession } from './session';
import { clerkConfigured } from '@/lib/clerk-config';
import { Brand } from './brand';
export function AuthForm({signup=false,inline=false,selectedPlan=''}:{signup?:boolean;inline?:boolean;selectedPlan?:string}){
 const [error,setError]=useState('');const [busy,setBusy]=useState(false);const [visible,setVisible]=useState(false);const router=useRouter();const session=useSession();
 return <section className={inline?'auth-inline':'auth-page'}><div className="auth-art"><Brand/><div><span className="kicker">LESS ADMIN. MORE TOGETHER.</span><h1>A little space<br/>for your next<br/><em>big thing.</em></h1><p>Your people, their free time, and the perfect room.<br/>Finally on the same page.</p></div><div className="paper-art" aria-hidden="true"><span className="paper-one">Your time.</span><span className="paper-two">Your people.</span><span className="paper-three">Your place. ✳</span></div></div><div className="auth-form-wrap"><Link href="/" className="back-link">← Back to home</Link><div className="auth-form-content"><span className="kicker">{signup?'MAKE ROOM FOR POSSIBILITY':'YOUR NEXT GOOD IDEA STARTS HERE'}</span><h2>{signup?'Create your workspace.':'Welcome back.'}</h2><p>{signup?'Explore your own workspace before choosing a plan.':'Sign in to pick up where you left off.'}</p>
 <form className="product-form" onSubmit={async e=>{e.preventDefault();setError('');setBusy(true);const form=new FormData(e.currentTarget);try{await request(signup?'/auth/signup':'/auth/login',{method:'POST',body:JSON.stringify({email:form.get('email'),password:form.get('password'),...(signup?{name:form.get('name'),workspaceName:form.get('workspaceName'),timezone:Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'}:{})})});await session.refresh();if(!inline)router.push(selectedPlan?'/app/billing':'/app');}catch(e){setError(e instanceof Error?e.message:'Unable to sign in.');}finally{setBusy(false);}}}>
 {signup&&<><Label htmlFor="account-name">Your name</Label><Input id="account-name" name="name" required maxLength={80} autoComplete="name" placeholder="What should we call you?"/><Label htmlFor="workspace-name">Workspace name</Label><Input id="workspace-name" name="workspaceName" required maxLength={100} placeholder="Your team, studio, or community"/></>}
 <Label htmlFor="email">Email</Label><Input id="email" name="email" type="email" required maxLength={254} autoComplete="email" placeholder="you@yourteam.com"/>
 <Label htmlFor="password">Password</Label><div className="password-field"><Input id="password" name="password" type={visible?'text':'password'} required minLength={signup?12:1} maxLength={72} autoComplete={signup?'new-password':'current-password'} placeholder={signup?'At least 12 characters':'Your password'}/><button type="button" onClick={()=>setVisible(!visible)} aria-label={visible?'Hide password':'Show password'}>{visible?<EyeOff size={17}/>:<Eye size={17}/>}</button></div>
 {error&&<p role="alert" className="form-error">{error}</p>}<Button disabled={busy} type="submit" className="auth-submit">{busy?<LoaderCircle size={18} className="spin"/>:null}{signup?'Create preview workspace':'Sign in'}<ArrowRight size={17}/></Button></form>
 {!signup&&<Link href="/forgot-password" className="forgot-link">Forgot password?</Link>}{clerkConfigured()&&<div className="social-sign-in"><Button variant="outline" asChild><Link href="/oauth">Continue with a social account</Link></Button></div>}
 <p className="auth-switch">{signup?'Already have an account?':'New here?'} <Link href={signup?'/login':'/signup'}>{signup?'Sign in':'Create a workspace'}</Link></p><small>Your team directory is yours to manage. Adding a person does not email or invite them.</small><p className="auth-legal">Read our <Link href="/terms">Terms</Link> and <Link href="/privacy">Privacy policy</Link>. Need a hand? <Link href="/contact">Contact us</Link>.</p></div></div></section>;
}
