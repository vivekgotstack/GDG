import { clerkMiddleware } from '@clerk/nextjs/server';
import { NextResponse,type NextRequest,type NextFetchEvent } from 'next/server';
import { clerkConfigured } from '@/lib/clerk-config';
const social=clerkMiddleware();
export default function proxy(request:NextRequest,event:NextFetchEvent){
 if(!clerkConfigured()||!process.env.CLERK_SECRET_KEY||process.env.CLERK_SECRET_KEY.startsWith('REPLACE_'))return NextResponse.next();
 return social(request,event);
}
export const config={matcher:['/oauth/:path*']};
