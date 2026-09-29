import Link from 'next/link';
import { product } from '@/lib/product';
export function Brand(){return <Link href="/" className="product-brand" aria-label={`${product.name} home`}><span className="brand-mark"><i/><i/><i/><i/></span>{product.name}<span className="brand-dot">✳</span></Link>;}
