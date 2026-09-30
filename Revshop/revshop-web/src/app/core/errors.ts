import { HttpErrorResponse } from '@angular/common/http';

const MAP: [RegExp, string][] = [
  [/already registered/i, 'That email is already registered. Log in instead.'],
  [/insufficient stock|out of stock|exceeds|not enough/i, 'Not enough stock available. Lower the quantity and try again.'],
  [/already reviewed/i, 'You have already reviewed this product. Edit your review instead.'],
  [/cart is empty/i, 'Your cart is empty. Add a product first.'],
  [/password must/i, 'Password must be at least 6 characters.'],
  [/cannot be cancelled|invalid status/i, 'This order can no longer change to that status.'],
];
const SVC: [string, string][] = [['/users', 'user'], ['/api/', 'product'], ['/cart', 'cart'], ['/orders', 'order'], ['/payments', 'payment'], ['/notifications', 'notification']];
const serviceOf = (url: string | null) => SVC.find(([p]) => (url ?? '').includes(p))?.[1] ?? 'server';

/** Turns any failed request into a sentence a shopper can act on. Never shows raw backend text. */
export function friendly(e: unknown, fallback = 'Something went wrong. Try again.', byStatus: Record<number, string> = {}): string {
  if (!(e instanceof HttpErrorResponse)) return fallback;
  if (e.status === 0 || [502, 503, 504].includes(e.status)) return `Can’t reach the ${serviceOf(e.url)} service. Make sure it is running, then try again.`;
  if (byStatus[e.status]) return byStatus[e.status];
  const body = e.error;
  const msg = typeof body === 'string' ? body : typeof body?.message === 'string' ? body.message : '';
  for (const [re, text] of MAP) if (re.test(msg)) return text;
  if (e.status === 401) return 'Your session has expired. Log in again.';
  if (e.status === 403) return 'You don’t have permission to do that.';
  if (e.status === 404) return 'We couldn’t find that.';
  return fallback;
}
