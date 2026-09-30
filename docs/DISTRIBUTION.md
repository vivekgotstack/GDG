# Installable apps

## Mobile PWA

The production Next.js build supplies `/manifest.webmanifest`, generated purple icons, and `/sw.js`. Registration runs only in production over HTTPS (or localhost). Settings contains the install action and iOS instructions. Workspace data requires a connection; the service worker caches only the public offline page/icons. No account API, private HTML, token, or write is cached. Rebuild/redeploy normally to update the web app.

Test `npm run build` followed by `npm start` with the API configured. On Android/desktop use Install app; on iOS use Safari → Share → Add to Home Screen. Installation eligibility is controlled by the browser.

## Tauri Windows desktop

The desktop package loads YOUR hosted Next.js origin because the Next API proxy and auth need a server. It does not bundle a local backend or database. Configure the hosted app first.

```powershell
cd frontend
npm ci
$env:MEETGRID_DESKTOP_URL='https://YOUR_FRONTEND_HOST'
npm run desktop:build
# Store installer variant (includes offline WebView2 runtime):
npm run desktop:store
```

Local Windows builds require Rust, MSVC C++ Build Tools, and WebView2. Alternatively run the manual **Build MeetGrid Windows installer** GitHub Actions workflow, supplying the HTTPS origin. It produces an installer artifact, not a published Store listing. Generated desktop icons use the same purple app mark. The shipped remote webview has no native IPC/file permissions.

The fallback window deliberately refuses to pretend it is a connected product when no production URL is configured. `desktop:build` refuses placeholder/non-HTTPS URLs. Use `npm run desktop:dev` with the local Next server running for development.

Microsoft Store release still requires Partner Center registration, reserved product identity, matching publisher details, signing/packaging decisions, screenshots, age/privacy declarations, and review. Do not advertise a Store download before approval. Verify password sign-in, downloads, email links, updates, and billing in the actual Windows build. Embedded webviews may be rejected by OAuth providers; the desktop wrapper currently supports password sign-in and does not implement a system-browser OAuth/deep-link bridge. Use the web app for social sign-in until that bridge is added.

A future Play Store release can package the hosted PWA as a Trusted Web Activity. It still requires a signing key, Digital Asset Links on your production domain, package identity, Play Console setup, and store-policy review. No Play release or native Android binary is claimed here.

References: [Tauri Microsoft Store guide](https://v2.tauri.app/distribute/microsoft-store/), [Next.js PWA guide](https://nextjs.org/docs/app/guides/progressive-web-apps).
