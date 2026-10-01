"use client";
import { createContext,useCallback,useContext,useEffect,useRef,useState } from 'react';
import { request } from '@/lib/api';
import type { Account } from '@/lib/product';
const SessionContext=createContext<{user:Account|null;loading:boolean;refresh:()=>Promise<Account|null>}>({user:null,loading:true,refresh:async()=>null});
export function SessionProvider({children}:{children:React.ReactNode}){
 const [user,setUser]=useState<Account|null>(null);const [loading,setLoading]=useState(true);const sequence=useRef(0);
 const refresh=useCallback(async()=>{const current=++sequence.current;try{const account=await request<Account>('/auth/me');if(current===sequence.current)setUser(account);return account;}catch{if(current===sequence.current)setUser(null);return null;}finally{if(current===sequence.current)setLoading(false);}},[]);
 useEffect(()=>{void refresh();const focus=()=>{void refresh();};window.addEventListener('focus',focus);return()=>window.removeEventListener('focus',focus);},[refresh]);
 return <SessionContext.Provider value={{user,loading,refresh}}>{children}</SessionContext.Provider>;
}
export const useSession=()=>useContext(SessionContext);
