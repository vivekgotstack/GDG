export const product = {
  name: process.env.NEXT_PUBLIC_APP_NAME || 'MeetGrid',
  tagline: process.env.NEXT_PUBLIC_APP_TAGLINE || 'Good company. Better coordination.',
  supportEmail: process.env.NEXT_PUBLIC_SUPPORT_EMAIL || 'vivekgotstack@gmail.com',
  supportPhone: process.env.NEXT_PUBLIC_SUPPORT_PHONE || '8303165648',
  companyName: process.env.NEXT_PUBLIC_COMPANY_NAME || 'StackOrcs',
  companyUrl: process.env.NEXT_PUBLIC_COMPANY_URL || 'https://stackorcs.com',
};
export type Account = {id:string;email:string;name:string;workspaceName:string;timezone:string;plan:string;role:'USER'|'ADMIN';emailVerified:boolean;hasPassword:boolean;socialLinked:boolean};
export type Plan = {id:string;name:string;description:string;monthlyPrice:number;currency:string;members:number;rooms:number;bookings:number;presets:number;checkoutEnabled:boolean;version:number;features?:string[]};
export type Preset = {id:string;name:string;description:string;durationMinutes:number;capacity:number};
