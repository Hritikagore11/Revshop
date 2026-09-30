import { Pipe, PipeTransform } from '@angular/core';
import { Order, OrderStatus, Product } from './models';

export const finalPrice = (p: Pick<Product, 'price' | 'discount'>) => p.price * (1 - (p.discount || 0) / 100);
export const money = (n: number | null | undefined) => '₹' + Number(n || 0).toLocaleString('en-IN', { maximumFractionDigits: 2 });
export const fdate = (t: string) => new Date(t).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
export const orderRef = (o: Order) => `ORD-${new Date(o.createdAt).getFullYear()}-${String(o.id).padStart(5, '0')}`;
export const isLow = (p: Product) => p.quantity > 0 && p.quantity <= (p.lowStockThreshold ?? 5);
export const stockOf = (p: Product): { kind: string; text: string } =>
  p.quantity <= 0 ? { kind: 'bad', text: 'Out of stock' } : isLow(p) ? { kind: 'warn', text: `${p.quantity} left` } : { kind: 'ok', text: 'In stock' };
export const STATUS: Record<OrderStatus, { kind: string; text: string }> = {
  PLACED: { kind: 'neutral', text: 'Placed' }, CONFIRMED: { kind: 'warn', text: 'Confirmed' }, SHIPPED: { kind: 'ok', text: 'Shipped' },
  DELIVERED: { kind: 'ok', text: 'Delivered' }, CANCELLED: { kind: 'bad', text: 'Cancelled' }, PAYMENT_FAILED: { kind: 'bad', text: 'Payment failed' },
};
export const byNewest = <T extends { createdAt: string }>(a: T, b: T) => +new Date(b.createdAt) - +new Date(a.createdAt);

@Pipe({ name: 'money' }) export class MoneyPipe implements PipeTransform { transform = money; }
@Pipe({ name: 'fdate' }) export class FdatePipe implements PipeTransform { transform = fdate; }
@Pipe({ name: 'price' }) export class PricePipe implements PipeTransform { transform(p: Product) { return money(finalPrice(p)); } }
@Pipe({ name: 'stars' }) export class StarsPipe implements PipeTransform {
  transform(n: number) { const r = Math.max(0, Math.min(5, Math.round(n))); return '★'.repeat(r) + '☆'.repeat(5 - r); }
}
@Pipe({ name: 'ago' }) export class AgoPipe implements PipeTransform {
  transform(t: string) {
    const m = (Date.now() - +new Date(t)) / 6e4;
    return m < 1 ? 'Just now' : m < 60 ? `${Math.floor(m)} min ago` : m < 1440 ? `${Math.floor(m / 60)} hours ago` : m < 2880 ? 'Yesterday' : fdate(t);
  }
}
export const PIPES = [MoneyPipe, FdatePipe, PricePipe, StarsPipe, AgoPipe] as const;
