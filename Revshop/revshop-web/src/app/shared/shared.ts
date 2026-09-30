import { ChangeDetectionStrategy, Component, HostListener, computed, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Product, OrderStatus } from '../core/models';
import { PIPES, STATUS, stockOf } from '../core/util';

@Component({
  selector: 'rs-thumb', changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (url() && !failed()) { <img [src]="url()" [alt]="alt()" loading="lazy" (error)="failed.set(true)"> }
    @else { <span class="img-fallback" aria-hidden="true">{{ initial() }}</span> }`,
  styles: `:host{display:contents}`,
})
export class Thumb {
  readonly url = input<string | null | undefined>(null);
  readonly name = input('');
  readonly alt = input('');
  readonly failed = signal(false);
  readonly initial = computed(() => (this.name() || '?').charAt(0));
}

@Component({
  selector: 'rs-status', changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="badge" [class]="s().kind">{{ s().text }}</span>`,
})
export class StatusBadge {
  readonly status = input.required<OrderStatus>();
  readonly s = computed(() => STATUS[this.status()] ?? { kind: 'neutral', text: this.status() });
}

@Component({
  selector: 'rs-product-card', imports: [RouterLink, Thumb, ...PIPES], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<a class="pc" [routerLink]="['/product', p().id]">
    <div class="img"><rs-thumb [url]="p().imageUrl" [name]="p().name" /></div>
    <div class="body"><div class="name">{{ p().name }}</div>
      <div class="pc-row"><span><span class="price">{{ p() | price }}</span>@if (p().discount! > 0) { <s class="was">{{ p().price | money }}</s> }</span>
        <span class="badge" [class]="stock().kind">{{ stock().text }}</span></div></div></a>`,
})
export class ProductCard {
  readonly p = input.required<Product>();
  readonly stock = computed(() => stockOf(this.p()));
}

@Component({
  selector: 'rs-modal', changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="overlay" (mousedown)="$event.target === $event.currentTarget && closed.emit()">
    <div class="modal" role="dialog" aria-modal="true" [attr.aria-label]="label()"><ng-content /></div></div>`,
})
export class Modal {
  readonly label = input('Dialog');
  readonly closed = output<void>();
  @HostListener('document:keydown.escape') esc() { this.closed.emit(); }
}

export const DELIVERY_FEE = 0; // order-service adds no delivery charge; keep 0 unless it does

@Component({
  selector: 'rs-summary', imports: [...PIPES], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<aside class="summary" aria-label="Order summary">
    <div class="line"><span>{{ count() }} item{{ count() === 1 ? '' : 's' }}</span><span class="price">{{ subtotal() | money }}</span></div>
    <div class="line"><span>Delivery</span><span class="price">{{ fee ? (fee | money) : 'Free' }}</span></div>
    <div class="line total"><span>Total</span><span class="price">{{ subtotal() + fee | money }}</span></div>
    <div style="margin-top:14px"><ng-content /></div></aside>`,
})
export class Summary {
  readonly count = input.required<number>();
  readonly subtotal = input.required<number>();
  readonly fee = DELIVERY_FEE;
}
