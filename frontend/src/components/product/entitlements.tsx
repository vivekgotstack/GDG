'use client';
import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import Link from 'next/link';
import { ArrowUpRight, LockKeyhole } from 'lucide-react';
import { request } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { useSession } from './session';

export type Entitlements = { planId: string; planName: string; admin: boolean; features: string[] };
const Context = createContext<{ access: Entitlements | null; error: string; reload: () => Promise<void> }>({ access: null, error: '', reload: async () => {} });
export function EntitlementProvider({ children }: { children: React.ReactNode }) {
  const { user } = useSession();
  const [access, setAccess] = useState<Entitlements | null>(null);
  const [error, setError] = useState('');
  const reload = useCallback(async () => {
    try { setAccess(await request<Entitlements>('/tools/entitlements')); setError(''); }
    catch (e) { setAccess(null); setError(e instanceof Error ? e.message : 'Plan access could not load.'); }
  }, []);
  useEffect(() => { setAccess(null); void reload(); }, [user?.id, user?.plan, user?.role, reload]);
  useEffect(() => { const refresh = () => { void reload(); }; window.addEventListener('focus', refresh); return () => window.removeEventListener('focus', refresh); }, [reload]);
  return <Context.Provider value={{ access, error, reload }}>{children}</Context.Provider>;
}
export const useEntitlements = () => useContext(Context);
export function FeatureGate({ feature, title, description, children }: { feature: string; title: string; description: string; children: React.ReactNode }) {
  const { access, error, reload } = useEntitlements();
  if (error) return <div className="feature-upgrade"><p role="alert">{error}</p><Button onClick={() => void reload()}>Retry plan access</Button></div>;
  if (!access) return <p role="status">Loading your plan access…</p>;
  if (access.features.includes(feature)) return <>{children}</>;
  const required = feature === 'bulk_import' ? 'Gather' : feature === 'insights' ? 'Studio' : 'Collective';
  return <section className="feature-upgrade"><span className="feature-lock"><LockKeyhole size={26}/></span><span className="kicker">MORE ROOM TO WORK</span><h2>{title}</h2><p>{description}</p><p>You’re on {access.planName}. This tool is included with {required}{feature !== 'operations_reports' ? ' and higher plans' : ''}.</p><Button asChild><Link href="/app/billing">Explore {required}<ArrowUpRight size={16}/></Link></Button></section>;
}
