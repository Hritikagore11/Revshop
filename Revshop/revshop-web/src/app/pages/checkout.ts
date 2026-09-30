import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Api } from '../core/api.service';
import { Auth } from '../core/auth.service';
import { Badges } from '../core/badges.service';
import { friendly } from '../core/errors';
import { CartItem, Product } from '../core/models';
import { Ui } from '../core/ui.service';
import { finalPrice } from '../core/util';
import { Summary } from '../shared/shared';

@Component({
  selector: 'rs-checkout', imports: [ReactiveFormsModule, Summary], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<h1>Checkout</h1>
    @if (loading()) { <p class="muted" role="status">Loading…</p> }
    @else if (loadError()) { <div class="alert error" role="alert">{{ loadError() }}</div> }
    @else {
    <div class="cc"><form id="ship" [formGroup]="form" (ngSubmit)="place()" novalidate>
      @if (error()) { <div class="alert error" role="alert">{{ error() }}</div> }
      <h3 style="margin-top:0">Shipping address</h3>
      <div class="field" [class.err]="bad('name')"><label for="name">Full name</label><input id="name" formControlName="name" autocomplete="name"><span class="help">{{ bad('name') ? 'Enter the recipient’s full name.' : '' }}</span></div>
      <div class="field" [class.err]="bad('address')"><label for="address">Address</label><input id="address" formControlName="address" autocomplete="street-address"><span class="help">{{ bad('address') ? 'Enter the street address.' : '' }}</span></div>
      <div class="split">
        <div class="field" [class.err]="bad('city')"><label for="city">City</label><input id="city" formControlName="city" autocomplete="address-level2"><span class="help">{{ bad('city') ? 'Enter your city.' : '' }}</span></div>
        <div class="field" [class.err]="bad('pin')"><label for="pin">PIN code</label><input id="pin" formControlName="pin" inputmode="numeric" maxlength="6" autocomplete="postal-code"><span class="help">{{ bad('pin') ? 'Enter a 6-digit PIN code.' : '' }}</span></div></div>
      <label class="row meta" style="margin-bottom:8px"><input type="checkbox" formControlName="save"> Save this address to my profile</label>
      <h3>Payment method</h3>
      <label class="pay-opt"><input type="radio" formControlName="pay" value="COD"> Cash on delivery</label>
      <label class="pay-opt"><input type="radio" formControlName="pay" value="CARD"> Credit / debit card (simulated)</label></form>
      <rs-summary [count]="count()" [subtotal]="subtotal()"><button class="btn btn-primary btn-block" form="ship" [disabled]="placing()">{{ placing() ? 'Placing order…' : 'Place order' }}</button></rs-summary></div> }`,
})
export class Checkout {
  private api = inject(Api); private auth = inject(Auth); private router = inject(Router); private ui = inject(Ui); private badges = inject(Badges); private fb = inject(FormBuilder);
  readonly loading = signal(true); readonly loadError = signal<string | null>(null); readonly error = signal<string | null>(null);
  readonly placing = signal(false); readonly submitted = signal(false);
  readonly items = signal<CartItem[]>([]); readonly prices = signal(new Map<number, Product>());
  readonly count = computed(() => this.items().reduce((s, i) => s + i.quantity, 0));
  readonly subtotal = computed(() => this.items().reduce((s, i) => { const p = this.prices().get(i.productId); return s + (p ? finalPrice(p) * i.quantity : 0); }, 0));
  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.pattern(/\S/)]], address: ['', [Validators.required, Validators.pattern(/\S/)]],
    city: ['', [Validators.required, Validators.pattern(/\S/)]], pin: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]], save: [true], pay: ['COD'],
  });
  constructor() { void this.init(); }
  bad(n: 'name' | 'address' | 'city' | 'pin') { const c = this.form.controls[n]; return c.invalid && (c.touched || this.submitted()); }
  private async init() {
    try {
      const [items, ps] = await Promise.all([firstValueFrom(this.api.cart()), firstValueFrom(this.api.products()), this.auth.profile() ? Promise.resolve() : this.auth.loadProfile()]);
      const map = new Map(ps.map(p => [p.id, p]));
      if (!items.length) { await this.router.navigate(['/cart']); return; }
      if (items.some(i => { const p = map.get(i.productId); return !p || p.quantity < i.quantity; })) {
        this.ui.show('Some items are no longer available. Update your cart first.', 'warn'); await this.router.navigate(['/cart']); return;
      }
      this.items.set(items); this.prices.set(map);
      const me = this.auth.profile(); this.form.patchValue({ name: me?.name ?? '', address: me?.address ?? '' });
    } catch (e) { this.loadError.set(friendly(e, 'We couldn’t load checkout — try again.')); }
    finally { this.loading.set(false); }
  }
  async place() {
    this.submitted.set(true); this.error.set(null);
    if (this.form.invalid || this.placing()) return;
    this.placing.set(true);
    const v = this.form.getRawValue();
    try {
      if (v.save) {
        await firstValueFrom(this.api.updateProfile({ name: v.name.trim(), phone: this.auth.profile()?.phone ?? '', address: `${v.address.trim()}, ${v.city.trim()} ${v.pin}` }))
          .then(() => this.auth.loadProfile()).catch(() => undefined); // saving the address must never block the order
      }
      await firstValueFrom(this.api.checkout(v.pay));
      this.ui.show('Order placed. Confirmation sent to your notifications.'); void this.badges.refresh();
      await this.router.navigate(['/orders']);
    } catch (e) {
      this.error.set(friendly(e, 'We couldn’t place your order. Check your cart and try again.', { 400: 'We couldn’t place your order. An item may be out of stock — review your cart and try again.' }));
      window.scrollTo({ top: 0 });
    } finally { this.placing.set(false); }
  }
}
