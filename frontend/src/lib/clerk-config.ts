export function clerkConfigured(){
 const key=process.env.NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY||'';
 return /^pk_(test|live)_/.test(key)&&!key.includes('REPLACE')&&!key.includes('placeholder');
}
