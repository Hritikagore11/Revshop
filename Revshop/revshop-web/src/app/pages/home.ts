import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Api } from '../core/api.service';
import { Auth } from '../core/auth.service';
import { friendly } from '../core/errors';
import { Category, Product } from '../core/models';
import { ProductCard } from '../shared/shared';

@Component({
  selector: 'rs-home', imports: [RouterLink, ProductCard], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="hero"><h1>Everything you need, from sellers you trust</h1>
      <p>Browse products across categories{{ auth.isBuyer() ? ', add to cart and check out in a few clicks' : ' and manage your listings from the seller dashboard' }}.</p>
      <a class="btn btn-primary" routerLink="/products">Browse products</a></div>
    @if (error()) { <div class="alert error" role="alert">{{ error() }} <button class="btn btn-secondary btn-sm" (click)="load()">Retry</button></div> }
    <div class="chips"><a class="chip active" routerLink="/products">All</a>
      @for (c of cats(); track c.id) { <a class="chip" routerLink="/products" [queryParams]="{ cat: c.id }">{{ c.name }}</a> }</div>
    <div class="section-h"><h2>Featured products</h2><a routerLink="/products">View all ›</a></div>
    @if (loading()) { <p class="muted" role="status">Loading…</p> }
    @else if (products().length) { <div class="grid g4">@for (p of products(); track p.id) { <rs-product-card [p]="p" /> }</div> }
    @else if (!error()) { <div class="empty"><p>No products yet — check back soon.</p></div> }`,
})
export class Home {
  private api = inject(Api); readonly auth = inject(Auth);
  readonly products = signal<Product[]>([]); readonly cats = signal<Category[]>([]);
  readonly loading = signal(true); readonly error = signal<string | null>(null);
  constructor() { void this.load(); }
  async load() {
    this.loading.set(true); this.error.set(null);
    try {
      const [pg, cs] = await Promise.all([firstValueFrom(this.api.productsPage(0, 8)), firstValueFrom(this.api.categories())]);
      this.products.set(pg.content ?? []); this.cats.set(cs);
    } catch (e) { this.error.set(friendly(e, 'We couldn’t load products — try again.')); }
    finally { this.loading.set(false); }
  }
}
