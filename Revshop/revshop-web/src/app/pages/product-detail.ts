import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Api } from '../core/api.service';
import { Auth } from '../core/auth.service';
import { Badges } from '../core/badges.service';
import { friendly } from '../core/errors';
import { Product, Review } from '../core/models';
import { PIPES, stockOf } from '../core/util';
import { Ui } from '../core/ui.service';
import { Thumb } from '../shared/shared';

@Component({
  selector: 'rs-product-detail', imports: [ReactiveFormsModule, RouterLink, Thumb, ...PIPES], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (loading()) { <p class="muted" role="status">Loading…</p> }
    @else if (error()) { <div class="alert error" role="alert">{{ error() }}</div><a class="btn btn-secondary" routerLink="/products">Back to products</a> }
    @else if (p(); as p) {
    <div class="detail"><div class="img detail-image"><rs-thumb [url]="p.imageUrl" [name]="p.name" [alt]="p.name" /></div>
      <div><h1 style="font-size:24px;margin-bottom:6px">{{ p.name }}</h1>
        <div class="stars" [attr.aria-label]="avg().toFixed(1) + ' out of 5'">@if (reviews().length) { {{ avg() | stars }} }
          <span class="meta">{{ reviews().length ? '(' + reviews().length + ' review' + (reviews().length > 1 ? 's' : '') + ')' : 'No reviews yet' }}</span></div>
        <div style="font-size:22px;margin:14px 0"><span class="price">{{ p | price }}</span>@if (p.discount! > 0) { <s class="was">{{ p.price | money }}</s> <span class="badge ok">{{ p.discount }}% off</span> }</div>
        <span class="badge" [class]="stock().kind">{{ p.quantity > 0 && stock().kind === 'ok' ? 'In stock — ' + p.quantity + ' available' : stock().text }}</span>
        <p class="muted" style="max-width:50ch;margin:16px 0">{{ p.description || 'No description provided.' }}</p>
        @if (auth.isBuyer()) {
          <div class="row" style="gap:16px;margin-bottom:20px">
            <div class="qty" role="group" aria-label="Quantity">
              <button type="button" (click)="step(-1)" [disabled]="qty() <= 1" aria-label="Decrease quantity">−</button>
              <span aria-live="polite">{{ qty() }}</span>
              <button type="button" (click)="step(1)" [disabled]="qty() >= p.quantity" aria-label="Increase quantity">+</button></div>
            @if (p.quantity > 0) { <button class="btn btn-primary" (click)="add(p)" [disabled]="busy()">{{ busy() ? 'Adding…' : 'Add to cart' }}</button> }
            @else { <button class="btn btn-disabled" disabled>Out of stock</button> }</div>
        } @else { <div class="alert warn">You’re logged in as a seller. Log in with a buyer account to add this to a cart.</div> }
        <h3>Reviews</h3>
        @for (r of reviews(); track r.id) {
          <div class="review"><div class="row"><span><b>{{ r.buyerId === auth.userId() ? 'You' : 'Buyer #' + r.buyerId }}</b> · <span class="stars">{{ r.rating | stars }}</span></span>
            @if (r.buyerId === auth.userId()) { <button class="btn btn-danger btn-sm" (click)="removeReview(r)">Delete</button> }</div>
            <div class="muted">{{ r.comment }}</div></div>
        } @empty { <p class="meta">No reviews yet — be the first to share how it went.</p> }
        @if (auth.isBuyer()) {
          <form [formGroup]="form" (ngSubmit)="saveReview(p)" novalidate style="margin-top:16px">
            @if (formError()) { <div class="alert error" role="alert">{{ formError() }}</div> }
            <div class="field"><label for="rating">Your rating</label>
              <select id="rating" formControlName="rating">@for (n of [5, 4, 3, 2, 1]; track n) { <option [ngValue]="n">{{ n }} — {{ n | stars }}</option> }</select></div>
            <div class="field" [class.err]="form.controls.comment.invalid"><label for="comment">Comment</label>
              <textarea id="comment" rows="3" formControlName="comment" maxlength="500"></textarea>
              <span class="help">{{ form.controls.comment.invalid ? 'Comments can be up to 500 characters.' : '' }}</span></div>
            <button class="btn btn-secondary" [disabled]="savingReview()">{{ mine() ? 'Update review' : 'Post review' }}</button>
          </form> } @else { <p class="meta" style="margin-top:16px">Only buyer accounts can write reviews.</p> }
      </div></div> }`,
})
export class ProductDetail {
  private api = inject(Api); private fb = inject(FormBuilder); private badges = inject(Badges); private route = inject(ActivatedRoute);
  readonly auth = inject(Auth); private ui = inject(Ui);
  readonly p = signal<Product | null>(null); readonly reviews = signal<Review[]>([]);
  readonly loading = signal(true); readonly error = signal<string | null>(null);
  readonly qty = signal(1); readonly busy = signal(false); readonly savingReview = signal(false); readonly formError = signal<string | null>(null);
  readonly stock = computed(() => (this.p() ? stockOf(this.p()!) : { kind: 'ok', text: '' }));
  readonly avg = computed(() => { const r = this.reviews(); return r.length ? r.reduce((s, x) => s + x.rating, 0) / r.length : 0; });
  readonly mine = computed(() => this.reviews().find(r => r.buyerId === this.auth.userId()));
  readonly form = this.fb.nonNullable.group({ rating: [5], comment: ['', Validators.maxLength(500)] });
  private seq = 0;

  constructor() { this.route.paramMap.pipe(takeUntilDestroyed()).subscribe(m => void this.load(Number(m.get('id')))); }

  private async load(id: number) {
    const run = ++this.seq;
    this.loading.set(true); this.error.set(null); this.qty.set(1);
    if (!Number.isInteger(id) || id < 1) { this.error.set('We couldn’t find that product.'); this.loading.set(false); return; }
    try {
      const [p, rv] = await Promise.all([firstValueFrom(this.api.product(id)), firstValueFrom(this.api.reviews(id))]);
      if (run !== this.seq) return;
      this.p.set(p); this.setReviews(rv);
    } catch (e) { if (run === this.seq) this.error.set(friendly(e, 'We couldn’t load this product — try again.')); }
    finally { if (run === this.seq) this.loading.set(false); }
  }
  private setReviews(rv: Review[]) {
    this.reviews.set(rv); const m = this.mine();
    this.form.reset({ rating: m?.rating ?? 5, comment: m?.comment ?? '' });
  }
  step(d: number) { this.qty.update(q => Math.min(Math.max(1, q + d), this.p()?.quantity || 1)); }
  async add(p: Product) {
    this.busy.set(true);
    try { await firstValueFrom(this.api.addToCart(p.id, this.qty())); this.ui.show('Added to cart.'); void this.badges.refresh(); }
    catch (e) { this.ui.error(e, 'We couldn’t add this to your cart — try again.', { 400: 'Not enough stock available. Lower the quantity and try again.' }); }
    finally { this.busy.set(false); }
  }
  async saveReview(p: Product) {
    this.formError.set(null);
    if (this.form.invalid || this.savingReview()) return;
    this.savingReview.set(true);
    const { rating, comment } = this.form.getRawValue(); const body = { rating, comment: comment.trim() }; const m = this.mine();
    try {
      await firstValueFrom(m ? this.api.updateReview(m.id, body) : this.api.addReview(p.id, body));
      this.setReviews(await firstValueFrom(this.api.reviews(p.id))); this.ui.show(m ? 'Review updated.' : 'Review posted.');
    } catch (e) { this.formError.set(friendly(e, 'We couldn’t save your review — try again.', { 400: 'You have already reviewed this product. Edit your review instead.', 409: 'You have already reviewed this product. Edit your review instead.' })); }
    finally { this.savingReview.set(false); }
  }
  async removeReview(r: Review) {
    if (!(await this.ui.confirm('Delete your review?', 'Delete review'))) return;
    try { await firstValueFrom(this.api.deleteReview(r.id)); this.setReviews(await firstValueFrom(this.api.reviews(this.p()!.id))); this.ui.show('Review deleted.'); }
    catch (e) { this.ui.error(e, 'We couldn’t delete your review — try again.'); }
  }
}
