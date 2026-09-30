'use client';
import { SignIn,useAuth,useClerk } from '@clerk/nextjs';
import Link from 'next/link';
import { useState } from 'react';
import { clerkConfigured } from '@/lib/clerk-config';
import { request } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Brand } from './brand';
import { useSession } from './session';
function Frame({children}:{children:React.ReactNode}){return <main className="account-flow"><Brand/><div className="account-flow-card"><span className="kicker">ONE LESS PASSWORD TO REMEMBER</span><h1>Your account.<br/><em>Your way in.</em></h1>{children}<Link href="/login" className="flow-back">← Email & password sign-in</Link></div></main>;}
function Unavailable(){return <Frame><p>Social sign-in is being connected. You can use your email and password in the meantime.</p></Frame>;}
function Entry(){const {isLoaded,isSignedIn}=useAuth();return <Frame>{!isLoaded?<p role="status">Preparing secure sign-in…</p>:isSignedIn?<Button asChild><Link href="/oauth/complete">Continue to your workspace</Link></Button>:<SignIn routing="hash" withSignUp forceRedirectUrl="/oauth/complete" signUpForceRedirectUrl="/oauth/complete"/>}</Frame>;}
export function OAuthEntry(){return clerkConfigured()?<Entry/>:<Unavailable/>;}
function Complete(){const {isLoaded,isSignedIn,getToken}=useAuth();const clerk=useClerk();const {refresh}=useSession();const [busy,setBusy]=useState(false);const [error,setError]=useState('');return <Frame><p>Confirm this sign-in to open your private MeetGrid workspace. If you signed in with a password first, this links your social account.</p>{error&&<p className="form-error" role="alert">{error}</p>}{isLoaded&&!isSignedIn?<Button asChild><Link href="/oauth">Start social sign-in</Link></Button>:<Button disabled={busy||!isLoaded} onClick={async()=>{setBusy(true);setError('');try{const token=await getToken();if(!token)throw new Error('Your social sign-in expired. Please start again.');await request('/auth/clerk',{method:'POST',body:JSON.stringify({token})});await refresh();await clerk.signOut({redirectUrl:'/app'});}catch(e){setError(e instanceof Error?e.message:'Could not complete sign-in.');}finally{setBusy(false);}}}>{busy?'Opening your workspace…':'Continue to workspace'}</Button>}</Frame>;}
export function OAuthComplete(){return clerkConfigured()?<Complete/>:<Unavailable/>;}
