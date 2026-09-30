import { product } from './product';
import { policies } from './legal-content';
export const publicPages: Record<string, { title: string; description: string }> = {
  'how-it-works': { title: 'How it works', description: 'Create your directory, set up spaces, find shared time, and book a recurring meeting.' },
  'use-cases': { title: 'Use cases', description: 'Scheduling for student clubs, creative studios, coworking spaces, and small businesses.' },
  about: { title: 'About', description: `A thoughtful space for people, places, and shared time. Meet ${product.name}, in association with ${product.companyName}.` },
  contact: { title: 'Contact', description: `Contact ${product.name} for product support, billing, privacy, or partnerships. Email ${product.supportEmail} or call ${product.supportPhone}.` },
  help: { title: 'Help centre', description: 'Get started with your workspace, find meeting times, manage rooms, and understand subscriptions.' },
  legal: { title: 'Legal centre', description: 'Privacy, terms, cookies, refunds, security, and accessibility, together in one clear place.' },
  ...Object.fromEntries(Object.entries(policies).map(([slug, policy]) => [slug, { title: policy.title, description: policy.description }])),
};
