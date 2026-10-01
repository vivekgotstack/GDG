"use client";
import { createContext,useCallback,useContext,useEffect,useState } from 'react';
import { request } from '@/lib/api';
import type { Account } from '@/lib/product';
const SessionContext=createContext<{user:Account|null;loading:boolean;refresh:()=>Promise<void>}>({user:null,loading:true,refresh:async()=>{}});
export function SessionProvider({children}:{children:React.ReactNode}){
 const [user,setUser]=useState<Account|null>(null);const [loading,setLoading]=useState(true);
 const refresh=useCallback(async()=>{try{setUser(await request<Account>('/auth/me'));}catch{setUser(null);}finally{setLoading(false);}},[]);
 useEffect(()=>{void refresh();const focus=()=>{void refresh();};window.addEventListener('focus',focus);return()=>window.removeEventListener('focus',focus);},[refresh]);
 return <SessionContext.Provider value={{user,loading,refresh}}>{children}</SessionContext.Provider>;
}
export const useSession=()=>useContext(SessionContext);
