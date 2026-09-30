import { ClerkProvider } from '@clerk/nextjs';
import { clerkConfigured } from '@/lib/clerk-config';
export default function OAuthLayout({children}:{children:React.ReactNode}){
 if(!clerkConfigured())return children;
 return <ClerkProvider appearance={{variables:{colorPrimary:'#6756A7',colorBackground:'#FFFFFF',colorText:'#2F2F35',borderRadius:'12px',fontFamily:'Poppins, sans-serif'}}}>{children}</ClerkProvider>;
}
