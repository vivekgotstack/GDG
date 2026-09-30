import type { Metadata } from "next";
import "@fontsource/poppins/latin-400.css";
import "@fontsource/poppins/latin-500.css";
import "@fontsource/poppins/latin-600.css";
import "@fontsource/poppins/latin-700.css";
import "./globals.css";
import "./pastel.css";
import "./product.css";
import "./public-pages.css";
import "./admin-tools.css";
import { SessionProvider } from '@/components/product/session';
import { SiteProvider } from '@/components/product/site-provider';
export const metadata: Metadata = {
  title: `${process.env.NEXT_PUBLIC_APP_NAME || 'MeetGrid'} — Find your common ground`,
  description: process.env.NEXT_PUBLIC_APP_TAGLINE || 'One thoughtful workspace for team availability, rooms, and recurring meetings.',
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body><SiteProvider><SessionProvider>{children}</SessionProvider></SiteProvider></body></html>;
}
