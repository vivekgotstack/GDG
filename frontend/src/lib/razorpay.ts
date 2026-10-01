export type RazorpayResult = {
  razorpay_payment_id: string;
  razorpay_subscription_id: string;
  razorpay_signature: string;
};
export type RazorpayOptions = {
  key: string;
  subscription_id: string;
  name: string;
  description: string;
  image: string;
  prefill: { name: string; email: string };
  theme: { color: string };
  handler: (result: RazorpayResult) => void;
  modal: { confirm_close: boolean; ondismiss: () => void };
};
export type RazorpayCheckout = {
  open: () => void;
  on: (event: 'payment.failed', handler: (failure: { error?: { description?: string } }) => void) => void;
};
declare global {
  interface Window {
    Razorpay?: new (options: RazorpayOptions) => RazorpayCheckout;
  }
}
export type BillingCheckout = { keyId: string; subscriptionId: string; planName: string; amount: number; currency: string; cycles: number };
export type BillingSubscription = {
  subscriptionId: string;
  planId: string;
  status: string;
  paidUntil: string | null;
  currentEnd: string | null;
  cancelAtPeriodEnd: boolean;
  pendingPlanId: string | null;
  paymentMethod: string | null;
  canChangePlan: boolean;
  amount: number;
  currency: string;
  lastPaymentId: string | null;
};
