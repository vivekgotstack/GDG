import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { InformationPage } from '@/components/product/public-pages';
import { publicPages } from '@/lib/public-pages';
import { product } from '@/lib/product';

export const dynamicParams = false;
export function generateStaticParams() {
  return Object.keys(publicPages).map(page => ({ page }));
}

export async function generateMetadata({ params }: { params: Promise<{ page: string }> }): Promise<Metadata> {
  const { page } = await params;
  if (!Object.hasOwn(publicPages, page)) notFound();
  return { title: `${publicPages[page].title} — ${product.name}`, description: publicPages[page].description };
}

export default async function Page({ params }: { params: Promise<{ page: string }> }) {
  const { page } = await params;
  if (!Object.hasOwn(publicPages, page)) notFound();
  return <InformationPage page={page}/>;
}
