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
    const upstream = await fetch(url, {
      method: request.method,
      headers: { 'Content-Type': 'application/json', Accept: 'application/json',
        Cookie: request.headers.get('cookie')?.split(';').filter(c=>c.trim().startsWith('JSESSIONID=')).join(';') || '',
        'X-CSRF-TOKEN': request.headers.get('x-csrf-token') || '' },
      body: ['GET', 'HEAD'].includes(request.method) ? undefined : await request.text(),
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
    return response;
  } catch {
    return Response.json({ detail: 'The meeting service is waking up or temporarily unavailable. Please try again shortly.' }, { status: 502 });
  }
}

export { proxy as GET, proxy as POST, proxy as PUT, proxy as DELETE };
