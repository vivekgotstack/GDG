'use client';
import { createContext,useCallback,useContext,useEffect,useState } from 'react';
import { request } from '@/lib/api';
import { product } from '@/lib/product';
import { defaultCopy,type SiteSnapshot } from '@/lib/site-fields';
const initial:SiteSnapshot={version:0,values:{}};
const SiteContext=createContext({snapshot:initial,brand:product,text:(key:string,fallback='')=>defaultCopy[key]??fallback,update:(_value:SiteSnapshot)=>{}});
export function SiteProvider({children}:{children:React.ReactNode}){
 const [snapshot,setSnapshot]=useState(initial);
 useEffect(()=>{let active=true;const load=()=>{void request<SiteSnapshot>('/site').then(data=>{if(active)setSnapshot(data);}).catch(()=>{});};load();window.addEventListener('focus',load);return()=>{active=false;window.removeEventListener('focus',load);};},[]);
 const update=useCallback((value:SiteSnapshot)=>setSnapshot(value),[]);
 const brand={...product,...Object.fromEntries(Object.keys(product).map(key=>[key,snapshot.values[`brand.${key}`]||product[key as keyof typeof product]]))} as typeof product;
 const text=(key:string,fallback='')=>snapshot.values[key]??defaultCopy[key]??fallback;
 return <SiteContext.Provider value={{snapshot,brand,text,update}}>{children}</SiteContext.Provider>;
}
export const useSite=()=>useContext(SiteContext);
export function SiteText({name,children}:{name:string;children?:React.ReactNode}){const {text}=useSite();return <>{text(name,typeof children==='string'?children:'')||children}</>;}
