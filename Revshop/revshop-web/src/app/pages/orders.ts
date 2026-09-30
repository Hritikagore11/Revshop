import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Api } from '../core/api.service';
import { friendly } from '../core/errors';
import { Order, OrderItem, Payment, Product } from '../core/models';
import { Ui } from '../core/ui.service';
import { PIPES, byNewest, orderRef } from '../core/util';
import { Modal, StatusBadge } from '../shared/shared';

interface Detail { order: Order; items: OrderItem[]; names: Map<number, string>; pay: Payment | null }

@Component({
  selector: 'rs-orders', imports: [RouterLink, Modal, StatusBadge, ...PIPES], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<h1>Your orders</h1>
    @if (loading()) { <p class="muted" role="status">Loading…</p> }
    @else if (error()) { <div class="alert error" role="alert">{{ error() }} <button class="btn btn-secondary btn-sm" (click)="load()">Retry</button></div> }
    @else if (!orders().length) { <div class="empty"><p>No orders yet — browse products to get started.</p><a class="btn btn-primary" routerLink="/products">Browse products</a></div> }
    @else { @for (o of orders(); track o.id) {
      <div class="order-row"><div><span class="mono">{{ ref(o) }}</span><div class="meta">Placed {{ o.createdAt | fdate }}</div></div>
        <span class="price">{{ o.totalAmount | money }}</span><rs-status [status]="o.status" />
        <button class="btn btn-secondary btn-sm" (click)="open(o)" [disabled]="opening() === o.id">{{ opening() === o.id ? 'Loading…' : 'View details' }}</button></div> } }
    @if (detail(); as d) {
      <rs-modal [label]="'Order ' + ref(d.order)" (closed)="detail.set(null)">
        <h2><span class="mono">{{ ref(d.order) }}</span></h2>
        <div class="row" style="margin-bottom:12px"><rs-status [status]="d.order.status" /><span class="meta">Placed {{ d.order.createdAt | fdate }}</span></div>
        <div class="tbl"><table><thead><tr><th>Item</th><th class="num">Qty</th><th class="num">Price</th></tr></thead><tbody>
          @for (i of d.items; track i.id) { <tr><td>{{ d.names.get(i.productId) ?? 'Product #' + i.productId }}</td><td class="num">{{ i.quantity }}</td><td class="num">{{ i.price * i.quantity | money }}</td></tr> }
          <tr><td colspan="2"><b>Total</b></td><td class="num"><b>{{ d.order.totalAmount | money }}</b></td></tr></tbody></table></div>
        @if (d.pay; as pay) { <p class="meta">Paid by {{ pay.paymentMethod === 'COD' ? 'cash on delivery' : 'card' }} · {{ pay.status.toLowerCase() }}<br>Transaction <span class="mono" style="font-size:12px">{{ pay.transactionId }}</span></p> }
        <div class="row end"><button class="btn btn-secondary" (click)="detail.set(null)">Close</button>
          @if (canCancel(d.order)) { <button class="btn btn-danger" (click)="cancel(d.order)" [disabled]="cancelling()">{{ cancelling() ? 'Cancelling…' : 'Cancel order' }}</button> }</div>
      </rs-modal> }`,
})
export class Orders {
  private api = inject(Api); private ui = inject(Ui);
  readonly ref = orderRef;
  readonly orders = signal<Order[]>([]); readonly loading = signal(true); readonly error = signal<string | null>(null);
  readonly detail = signal<Detail | null>(null); readonly opening = signal<number | null>(null); readonly cancelling = signal(false);
  constructor() { void this.load(); }
  canCancel(o: Order) { return !['DELIVERED', 'CANCELLED', 'PAYMENT_FAILED'].includes(o.status); }
  async load() {
    this.error.set(null);
    try { this.orders.set((await firstValueFrom(this.api.orders())).sort(byNewest)); }
    catch (e) { this.error.set(friendly(e, 'We couldn’t load your orders — try again.')); }
    finally { this.loading.set(false); }
  }
  async open(order: Order) {
    this.opening.set(order.id);
    try {
      const [items, ps, pay] = await Promise.all([
        firstValueFrom(this.api.orderItems(order.id)),
        firstValueFrom(this.api.products()).catch(() => [] as Product[]),
        firstValueFrom(this.api.payment(order.id)).catch(() => null),
      ]);
      this.detail.set({ order, items, names: new Map(ps.map(p => [p.id, p.name])), pay });
    } catch (e) { this.ui.error(e, 'We couldn’t load this order — try again.'); }
    finally { this.opening.set(null); }
  }
  async cancel(o: Order) {
    this.detail.set(null);
    if (!(await this.ui.confirm('Cancel this order? Items go back into stock.', 'Cancel order'))) return;
    this.cancelling.set(true);
    try { await firstValueFrom(this.api.setStatus(o.id, 'CANCELLED')); this.ui.show('Order cancelled.'); await this.load(); }
    catch (e) { this.ui.error(e, 'We couldn’t cancel this order — try again.'); }
    finally { this.cancelling.set(false); }
  }
}
