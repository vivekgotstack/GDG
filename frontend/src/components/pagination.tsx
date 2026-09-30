'use client';
import { useState } from 'react';
import { Button } from './ui/button';
export function usePagination<T>(items:T[],size=12,key=''){
 const [position,setPosition]=useState({page:1,key});const pages=Math.max(1,Math.ceil(items.length/size));const page=position.key===key?Math.min(position.page,pages):1;
 return {items:items.slice((page-1)*size,page*size),page,pages,total:items.length,size,setPage:(value:number)=>setPosition({page:Math.max(1,Math.min(value,pages)),key})};
}
export function Pagination({page,pages,total,size,setPage,label='items'}:{page:number;pages:number;total:number;size:number;setPage:(n:number)=>void;label?:string}){
 if(total<=size)return null;
 return <nav className="list-pagination" aria-label={`${label} pages`}><span role="status">{(page-1)*size+1}–{Math.min(page*size,total)} of {total} {label}</span><div><Button variant="outline" size="sm" disabled={page===1} onClick={()=>setPage(page-1)}>Previous</Button><span>Page {page} / {pages}</span><Button variant="outline" size="sm" disabled={page===pages} onClick={()=>setPage(page+1)}>Next</Button></div></nav>;
}
