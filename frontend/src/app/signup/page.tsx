import { AuthForm } from '@/components/product/auth-form';
export default async function Signup({searchParams}:{searchParams:Promise<{plan?:string}>}){const {plan}=await searchParams;return <AuthForm signup selectedPlan={plan&&['starter','studio','scale'].includes(plan)?plan:''}/>;}
