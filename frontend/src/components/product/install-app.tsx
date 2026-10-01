'use client';
import { useEffect, useState, useSyncExternalStore } from 'react';
import { Download } from 'lucide-react';
import { Button } from '@/components/ui/button';
type InstallPrompt=Event&{prompt:()=>Promise<void>;userChoice:Promise<{outcome:string}>};
// Capture at the root: Settings may open long after the browser offers installation.
let pendingPrompt: InstallPrompt | null = null;
const listeners = new Set<() => void>();
function setPrompt(value: InstallPrompt | null) {
  pendingPrompt = value;
  listeners.forEach(listener => listener());
}
function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => { listeners.delete(listener); };
}
const getPrompt = () => pendingPrompt;
const getServerPrompt = () => null;

export function PwaRegistration() {
  useEffect(() => {
    const capture = (event: Event) => {
      event.preventDefault();
      setPrompt(event as InstallPrompt);
    };
    const installed = () => setPrompt(null);
    window.addEventListener('beforeinstallprompt', capture);
    window.addEventListener('appinstalled', installed);
    if (process.env.NODE_ENV === 'production' && 'serviceWorker' in navigator) {
      void navigator.serviceWorker.register('/sw.js', { scope: '/', updateViaCache: 'none' }).catch(() => {});
    }
    return () => {
      window.removeEventListener('beforeinstallprompt', capture);
      window.removeEventListener('appinstalled', installed);
    };
  }, []);
  return null;
}

export function InstallApp() {
  const prompt = useSyncExternalStore(subscribe, getPrompt, getServerPrompt);
  const [installed, setInstalled] = useState(false);
  const [installing, setInstalling] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    const media = window.matchMedia('(display-mode: standalone)');
    const update = () => setInstalled(media.matches || (navigator as Navigator & { standalone?: boolean }).standalone === true);
    const done = () => setInstalled(true);
    update();
    media.addEventListener('change', update);
    window.addEventListener('appinstalled', done);
    return () => {
      media.removeEventListener('change', update);
      window.removeEventListener('appinstalled', done);
    };
  }, []);
  async function install() {
    if (!prompt || installing) return;
    setInstalling(true);
    setError('');
    try {
      await prompt.prompt();
      await prompt.userChoice;
    } catch {
      setError('Installation could not open. Try your browser’s Install app menu.');
    } finally {
      setPrompt(null);
      setInstalling(false);
    }
  }
  return <section className="install-card">
    <Download size={24}/><h2>Make a little room on your home screen.</h2>
    <p>{installed ? 'MeetGrid is running as an installed app.' : 'Install MeetGrid for a focused app window and a home-screen shortcut. An internet connection is needed for your workspace.'}</p>
    {!installed && (prompt
      ? <Button disabled={installing} onClick={install}>{installing ? 'Opening installer…' : 'Install MeetGrid'}</Button>
      : <p>On iPhone or iPad, open Safari → Share → Add to Home Screen. On Android or desktop, look for “Install app” in your browser menu when available.</p>)}
    {error && <p role="alert">{error}</p>}
  </section>;
}
