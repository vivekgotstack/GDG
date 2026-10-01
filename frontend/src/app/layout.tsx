import type { Metadata, Viewport } from "next";
import "@fontsource/poppins/latin-400.css";
import "@fontsource/poppins/latin-500.css";
import "@fontsource/poppins/latin-600.css";
import "@fontsource/poppins/latin-700.css";
import "./globals.css";
import "./pastel.css";
import "./product.css";
import "./public-pages.css";
import "./admin-tools.css";
import "./account-flow.css";
import { SessionProvider } from '@/components/product/session';
import { SiteProvider } from '@/components/product/site-provider';
import { PwaRegistration } from '@/components/product/install-app';
export const viewport: Viewport = { themeColor: '#6756A7' };
export const metadata: Metadata = {
  appleWebApp:{capable:true,statusBarStyle:'default',title:'MeetGrid'},
  icons:{
    icon:[
      {url:'/icon.svg?v=meetgrid-purple-2',type:'image/svg+xml'},
      {url:'/icons/favicon-32.png?v=meetgrid-purple-2',type:'image/png',sizes:'32x32'},
    ],
    shortcut:'/favicon.ico?v=meetgrid-purple-2',
    apple:'/icons/apple-touch-icon.png',
  },
  title: `${process.env.NEXT_PUBLIC_APP_NAME || 'MeetGrid'} — Find your common ground`,
  description: process.env.NEXT_PUBLIC_APP_TAGLINE || 'One thoughtful workspace for team availability, rooms, and recurring meetings.',
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body><PwaRegistration/><SiteProvider><SessionProvider>{children}</SessionProvider></SiteProvider></body></html>;
}
