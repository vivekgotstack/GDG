import Link from 'next/link';
import { ArrowRight } from 'lucide-react';
import { PublicFooter, PublicNav } from '@/components/product/landing';
import { Button } from '@/components/ui/button';

export default function NotFound() {
  return <div className="public-site"><PublicNav/><main id="main-content" className="not-found-page"><span className="lost-page-art" aria-hidden="true">4<span>✳</span>4</span><span className="kicker">A LITTLE OFF THE CALENDAR</span><h1>This page hasn’t<br/><span className="serif-word">found its place.</span></h1><p>The address may have changed, or the page may not exist.<br/>Let’s find you some common ground.</p><div><Button asChild><Link href="/">Back to home<ArrowRight size={17}/></Link></Button><Link href="/help">Visit the help centre</Link></div></main><PublicFooter/></div>;
}
