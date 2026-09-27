export const runtime = "nodejs";
export const dynamic = "force-dynamic";
export const maxDuration = 180;

type Context = { params: Promise<{ path: string[] }> };

async function proxy(request: Request, context: Context) {
  const { path } = await context.params;
  try {
    const origin = new URL('https://gdg-sf6z.onrender.com');
    const url = new URL(`/api/${path.map(encodeURIComponent).join('/')}`, origin);
    url.search = new URL(request.url).search;
    const upstream = await fetch(url, {
      method: request.method,
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: ['GET', 'HEAD'].includes(request.method) ? undefined : await request.text(),
      cache: 'no-store',
      signal: AbortSignal.timeout(165000),
      redirect: 'error',
    });
    return new Response(upstream.status === 204 ? null : await upstream.arrayBuffer(), {
      status: upstream.status,
      headers: {
        'Content-Type': upstream.headers.get('content-type') || 'application/json',
        'Cache-Control': 'no-store',
      },
    });
  } catch {
    return Response.json({ detail: 'The meeting service is waking up or temporarily unavailable. Please try again shortly.' }, { status: 502 });
  }
}

export { proxy as GET, proxy as POST, proxy as PUT };
