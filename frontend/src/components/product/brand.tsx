'use client';
import Link from 'next/link';
import { useSite } from './site-provider';
export function Brand(){const {brand:product}=useSite();return <Link href="/" className="product-brand" aria-label={`${product.name} home`}><span className="brand-mark"><i/><i/><i/><i/></span>{product.name}<span className="brand-dot">✳</span></Link>;}
