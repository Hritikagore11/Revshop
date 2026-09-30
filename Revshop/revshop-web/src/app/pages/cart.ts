import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Api } from '../core/api.service';
import { Badges } from '../core/badges.service';
import { friendly } from '../core/errors';
import { CartItem, Product } from '../core/models';
import { PIPES, finalPrice } from '../core/util';
import { Ui } from '../core/ui.service';
import { Summary, Thumb } from '../shared/shared';

@Component({
  selector: 'rs-cart', imports: [RouterLink, Thumb, Summary, ...PIPES], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<h1>Your cart</h1>
    @if (loading()) { <p class="muted" role="status">Loading…</p> }
    @else if (error()) { <div class="alert error" role="alert">{{ error() }} <button class="btn btn-secondary btn-sm" (click)="load()">Retry</button></div> }
    @else if (!lines().length) { <div class="empty"><p>Your cart is empty — browse products to get started.</p><a class="btn btn-primary" routerLink="/products">Browse products</a></div> }
    @else {
      @if (blocked()) { <div class="alert warn" role="alert">Some items are no longer available in the quantity you chose. Update or remove them to continue.</div> }
      <div class="cc"><div>
        @for (l of lines(); track l.item.id) {
          <div class="cart-item"><div class="img"><rs-thumb [url]="l.p?.imageUrl" [name]="l.p?.name ?? '?'" /></div>
            <div><div style="font-weight:600;font-size:14px">@if (l.p) { <a [routerLink]="['/product', l.p.id]" style="color:inherit">{{ l.p.name }}</a> } @else { Unavailable product #{{ l.item.productId }} }</div>
              <div class="meta">{{ l.p ? (l.p | price) + ' each' : 'Remove this item to continue' }}</div></div>
            <div class="qty" role="group" aria-label="Quantity">
              <button type="button" (click)="setQty(l.item, l.item.quantity - 1)" [disabled]="busy() || l.item.quantity <= 1" aria-label="Decrease quantity">−</button>
              <span>{{ l.item.quantity }}</span>
              <button type="button" (click)="setQty(l.item, l.item.quantity + 1)" [disabled]="busy() || !l.p || l.item.quantity >= l.p.quantity" aria-label="Increase quantity">+</button></div>
            <span class="price">{{ l.p ? (l.line | money) : '—' }}</span>
            <button class="btn btn-danger btn-sm" (click)="remove(l.item)" [disabled]="busy()">Remove</button></div>
        }
        <div style="margin-top:16px"><button class="btn btn-secondary btn-sm" (click)="clear()" [disabled]="busy()">Clear cart</button></div></div>
        <rs-summary [count]="count()" [subtotal]="subtotal()">
          @if (blocked()) { <button class="btn btn-disabled btn-block" disabled>Checkout</button> } @else { <a class="btn btn-primary btn-block" routerLink="/checkout">Checkout</a> }
        </rs-summary></div> }`,
})
export class Cart {
  private api = inject(Api); private ui = inject(Ui); private badges = inject(Badges);
  readonly items = signal<CartItem[]>([]); readonly products = signal(new Map<number, Product>());
  readonly loading = signal(true); readonly busy = signal(false); readonly error = signal<string | null>(null);
  readonly lines = computed(() => this.items().map(item => { const p = this.products().get(item.productId); return { item, p, line: p ? finalPrice(p) * item.quantity : 0 }; }));
  readonly blocked = computed(() => this.lines().some(l => !l.p || l.p.quantity < l.item.quantity));
  readonly count = computed(() => this.items().reduce((s, i) => s + i.quantity, 0));
  readonly subtotal = computed(() => this.lines().reduce((s, l) => s + l.line, 0));
  constructor() { void this.load(); }
  async load() {
    this.error.set(null);
    try {
      const [items, ps] = await Promise.all([firstValueFrom(this.api.cart()), firstValueFrom(this.api.products())]);
      this.items.set(items); this.products.set(new Map(ps.map(p => [p.id, p]))); void this.badges.refresh();
    } catch (e) { this.error.set(friendly(e, 'We couldn’t load your cart — try again.')); }
    finally { this.loading.set(false); }
  }
  private async act(fn: () => Promise<unknown>, fallback: string, ok?: string) {
    this.busy.set(true);
    try { await fn(); if (ok) this.ui.show(ok); } catch (e) { this.ui.error(e, fallback, { 400: 'Not enough stock available for that quantity.' }); }
    try { await this.load(); } finally { this.busy.set(false); }
  }
  setQty(i: CartItem, q: number) { return this.act(() => firstValueFrom(this.api.setQuantity(i.id, q)), 'We couldn’t update the quantity — try again.'); }
  remove(i: CartItem) { return this.act(() => firstValueFrom(this.api.removeItem(i.id)), 'We couldn’t remove that item — try again.', 'Item removed from cart.'); }
  async clear() {
    if (await this.ui.confirm('Remove every item from your cart?', 'Clear cart')) await this.act(() => firstValueFrom(this.api.clearCart()), 'We couldn’t clear your cart — try again.', 'Cart cleared.');
  }
}
