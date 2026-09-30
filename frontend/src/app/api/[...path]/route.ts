import { createHmac } from 'node:crypto';
export const runtime = "nodejs";
export const dynamic = "force-dynamic";
export const maxDuration = 180;

type Context = { params: Promise<{ path: string[] }> };

async function proxy(request: Request, context: Context) {
  const { path } = await context.params;
  try {
    const base = process.env.API_BASE_URL || (process.env.NODE_ENV === 'development' ? 'http://127.0.0.1:8080' : '');
    if (!base) return Response.json({detail:'The API connection has not been configured.'},{status:503});
    const origin = new URL(base);
    const url = new URL(`/api/${path.map(encodeURIComponent).join('/')}`, origin);
    url.search = new URL(request.url).search;
    if(Number(request.headers.get('content-length'))>1048576)return Response.json({detail:'Request is too large.'},{status:413});
    const body=['GET','HEAD'].includes(request.method)?undefined:await request.text();
    if(body&&Buffer.byteLength(body)>1048576)return Response.json({detail:'Request is too large.'},{status:413});
    const proof:Record<string,string>={};
    const secret=process.env.PROXY_SHARED_SECRET||'';
    // Only use a client address supplied by a host that strips incoming spoofed headers.
    const ip=(process.env.VERCEL==='1'?request.headers.get('x-vercel-forwarded-for'):process.env.TRUST_PROXY_IP_HEADER==='true'?request.headers.get('x-forwarded-for'):null)?.split(',')[0].trim();
    if(ip&&ip.length<=80&&secret.length>=32&&!secret.startsWith('REPLACE_')){const time=Math.floor(Date.now()/1000).toString();proof['X-MeetGrid-Client']=ip;proof['X-MeetGrid-Time']=time;proof['X-MeetGrid-Signature']=createHmac('sha256',secret).update(ip+'\n'+time).digest('hex');}
    const upstream = await fetch(url, {
      method: request.method,
      headers: { 'Content-Type': 'application/json', Accept: 'application/json',
        Cookie: request.headers.get('cookie')?.split(';').filter(c=>c.trim().startsWith('JSESSIONID=')).join(';') || '',
        'X-CSRF-TOKEN': request.headers.get('x-csrf-token') || '',...proof },
      body,
      cache: 'no-store',
      signal: AbortSignal.timeout(165000),
      redirect: 'error',
    });
    const response = new Response(upstream.status === 204 ? null : await upstream.arrayBuffer(), {
      status: upstream.status,
      headers: {
        'Content-Type': upstream.headers.get('content-type') || 'application/json',
        'Cache-Control': 'no-store',
      },
    });
    for (const cookie of upstream.headers.getSetCookie()) response.headers.append('Set-Cookie',cookie);
    if(upstream.headers.has('retry-after'))response.headers.set('Retry-After',upstream.headers.get('retry-after')!);
    return response;
  } catch {
    return Response.json({ detail: 'The meeting service is waking up or temporarily unavailable. Please try again shortly.' }, { status: 502 });
  }
}

export { proxy as GET, proxy as POST, proxy as PUT, proxy as DELETE };
