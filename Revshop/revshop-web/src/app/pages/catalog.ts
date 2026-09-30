import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Api } from '../core/api.service';
import { friendly } from '../core/errors';
import { Category, Product } from '../core/models';
import { finalPrice } from '../core/util';
import { ProductCard } from '../shared/shared';

const SIZE = 12;
const PRICE: Record<string, (p: Product) => boolean> = {
  under: p => finalPrice(p) < 500, mid: p => finalPrice(p) >= 500 && finalPrice(p) <= 2000, over: p => finalPrice(p) > 2000,
};
const RANGES: [string, string][] = [['', 'Any price'], ['under', 'Under ₹500'], ['mid', '₹500 – ₹2,000'], ['over', 'Over ₹2,000']];

@Component({
  selector: 'rs-catalog', imports: [RouterLink, ProductCard], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<h1>Products</h1><div class="plist">
    <div class="filters"><h4>Category</h4>
      @for (c of cats(); track c.id) { <label><input type="checkbox" [checked]="sel().includes('' + c.id)" (change)="toggle(c.id, $any($event.target).checked)"> {{ c.name }}</label> }
      @empty { <span class="meta">No categories yet</span> }
      <h4>Price</h4>
      @for (r of ranges; track r[0]) { <label><input type="radio" name="pr" [checked]="pr() === r[0]" (change)="go({ pr: r[0] || null })"> {{ r[1] }}</label> }</div>
    <div>
      <form class="searchbar" (submit)="$event.preventDefault(); go({ q: q.value.trim() || null })" role="search">
        <input #q type="search" placeholder="Search products…" [value]="kw()" aria-label="Search products"><button class="btn btn-secondary">Search</button></form>
      @if (error()) { <div class="alert error" role="alert">{{ error() }} <button class="btn btn-secondary btn-sm" (click)="reload()">Retry</button></div> }
      @if (loading()) { <p class="muted" role="status">Loading…</p> }
      @else if (list().length) { <div class="grid g3">@for (p of list(); track p.id) { <rs-product-card [p]="p" /> }</div> }
      @else if (!error()) { <div class="empty"><p>No products match — clear the filters or try another search.</p><a class="btn btn-secondary" routerLink="/products">Clear filters</a></div> }
      @if (totalPages() > 1) {
        <nav class="pagination" aria-label="Pages">
          @if (page() > 0) { <a class="btn btn-secondary btn-sm" routerLink="/products" [queryParams]="qp(page() - 1)">‹ Previous</a> } @else { <span></span> }
          <span class="meta">Page {{ page() + 1 }} of {{ totalPages() }} · {{ total() }} products</span>
          @if (page() + 1 < totalPages()) { <a class="btn btn-secondary btn-sm" routerLink="/products" [queryParams]="qp(page() + 1)">Next ›</a> } @else { <span></span> }
        </nav> }
    </div></div>`,
})
export class Catalog {
  private api = inject(Api); private router = inject(Router); private route = inject(ActivatedRoute);
  readonly ranges = RANGES;
  readonly cats = signal<Category[]>([]); readonly list = signal<Product[]>([]);
  readonly kw = signal(''); readonly sel = signal<string[]>([]); readonly pr = signal(''); readonly page = signal(0);
  readonly totalPages = signal(1); readonly total = signal(0);
  readonly loading = signal(true); readonly error = signal<string | null>(null);
  private seq = 0;
  constructor() {
    firstValueFrom(this.api.categories()).then(c => this.cats.set(c)).catch(() => { /* filters just stay empty */ });
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(m => void this.load(m));
  }
  reload() { void this.load(this.route.snapshot.queryParamMap); }
  qp(page: number) { return { q: this.kw() || null, cat: this.sel().length ? this.sel() : null, pr: this.pr() || null, page: page || null }; }
  go(patch: Record<string, string | string[] | null>) { void this.router.navigate([], { queryParams: { ...this.qp(0), ...patch } }); }
  toggle(id: number, on: boolean) { const s = new Set(this.sel()); if (on) s.add(String(id)); else s.delete(String(id)); this.go({ cat: s.size ? [...s] : null }); }
  private async load(m: ParamMap) {
    const run = ++this.seq;
    const kw = m.get('q') ?? '', sel = m.getAll('cat'), pr = m.get('pr') ?? '';
    const page = Math.max(0, parseInt(m.get('page') ?? '0', 10) || 0);
    this.kw.set(kw); this.sel.set(sel); this.pr.set(pr); this.page.set(page); this.loading.set(true); this.error.set(null);
    try {
      let list: Product[], pages: number, total: number;
      if (!kw && !sel.length && !pr) {
        const pg = await firstValueFrom(this.api.productsPage(page, SIZE));
        list = pg.content ?? []; pages = pg.totalPages || 1; total = pg.totalElements ?? list.length;
      } else {
        // Filters run on the full list so a filtered page is never empty just because matches live on another server page.
        const all = (await firstValueFrom(kw ? this.api.search(kw) : this.api.products()))
          .filter(p => (!sel.length || sel.includes(String(p.category?.id))) && (!pr || PRICE[pr]?.(p)));
        total = all.length; pages = Math.max(1, Math.ceil(total / SIZE)); list = all.slice(page * SIZE, page * SIZE + SIZE);
      }
      if (run !== this.seq) return;
      this.list.set(list); this.totalPages.set(pages); this.total.set(total);
    } catch (e) {
      if (run === this.seq) { this.list.set([]); this.totalPages.set(1); this.error.set(friendly(e, 'We couldn’t load products — try again.')); }
    } finally { if (run === this.seq) this.loading.set(false); }
  }
}
