const CACHE='meetgrid-public-v1';
const SAFE=['/offline.html','/icons/icon-192.png','/icons/icon-512.png'];
self.addEventListener('install',event=>{event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(SAFE)));self.skipWaiting();});
self.addEventListener('activate',event=>{event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(key=>key.startsWith('meetgrid-public-')&&key!==CACHE).map(key=>caches.delete(key)))).then(()=>self.clients.claim()));});
self.addEventListener('fetch',event=>{
 const req=event.request,url=new URL(req.url);
 if(url.origin!==self.location.origin||req.method!=='GET')return;
 // No API responses, credentials, workspace HTML, email links, or mutations are cached.
 if(req.mode==='navigate'){event.respondWith(fetch(req).catch(()=>caches.match('/offline.html')));return;}
 if(SAFE.includes(url.pathname))event.respondWith(caches.match(req).then(hit=>hit||fetch(req)));
});
