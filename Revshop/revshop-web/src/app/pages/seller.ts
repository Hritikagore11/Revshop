import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Api } from '../core/api.service';
import { Auth } from '../core/auth.service';
import { friendly } from '../core/errors';
import { Category, Product, ProductPayload } from '../core/models';
import { Ui } from '../core/ui.service';
import { PIPES, isLow } from '../core/util';
import { Modal } from '../shared/shared';

@Component({
  selector: 'rs-seller', imports: [ReactiveFormsModule, RouterLink, Modal, ...PIPES], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<h1>Seller dashboard</h1>
    @if (loading()) { <p class="muted" role="status">Loading…</p> }
    @else if (error()) { <div class="alert error" role="alert">{{ error() }} <button class="btn btn-secondary btn-sm" (click)="load()">Retry</button></div> }
    @else {
      @for (p of low().slice(0, 3); track p.id) { <div class="alert warn">{{ p.name }} is {{ p.quantity ? 'low on stock (' + p.quantity + ' left)' : 'out of stock' }} — restock soon.</div> }
      @if (low().length > 3) { <div class="alert warn">{{ low().length - 3 }} more products need restocking.</div> }
      <div class="section-h"><h2>Your products</h2><button class="btn btn-primary btn-sm" (click)="openForm(null)">Add product</button></div>
      @if (mine().length) {
        <div class="tbl"><table><thead><tr><th>Product</th><th class="num">Price</th><th class="num">Stock</th><th>Status</th><th></th></tr></thead><tbody>
          @for (p of mine(); track p.id) {
            <tr><td><a [routerLink]="['/product', p.id]">{{ p.name }}</a></td><td class="num">{{ p | price }}</td><td class="num">{{ p.quantity }}</td>
              <td><span class="badge" [class]="state(p).kind">{{ state(p).text }}</span></td>
              <td class="row"><button class="btn btn-secondary btn-sm" (click)="openForm(p)">Edit</button><button class="btn btn-danger btn-sm" (click)="remove(p)">Delete</button></td></tr> }
        </tbody></table></div>
      } @else { <div class="empty"><p>No products yet — add your first product to start selling.</p></div> }

      <div class="section-h" style="margin-top:32px"><h2>Update an order</h2></div>
      <form class="panel row" [formGroup]="orderForm" (ngSubmit)="updateOrder()" novalidate style="align-items:flex-end">
        <div class="field" style="margin:0;width:140px" [class.err]="orderBad()"><label for="oid">Order number</label><input id="oid" type="number" min="1" formControlName="oid"><span class="help">{{ orderBad() ? 'Enter the order number.' : '' }}</span></div>
        <div class="field" style="margin:0;width:180px"><label for="st">New status</label><select id="st" formControlName="st"><option value="CONFIRMED">Confirmed</option><option value="SHIPPED">Shipped</option><option value="DELIVERED">Delivered</option></select></div>
        <button class="btn btn-secondary" [disabled]="busy()">Update status</button>
        <span class="meta" style="flex-basis:100%">Statuses move Placed → Confirmed → Shipped → Delivered, one step at a time.</span></form>

      <div class="section-h" style="margin-top:32px"><h2>Categories</h2></div>
      <form class="panel row" [formGroup]="catForm" (ngSubmit)="addCategory()" novalidate style="align-items:flex-end">
        <div class="field" style="margin:0;width:240px" [class.err]="catBad()"><label for="cname">New category</label><input id="cname" formControlName="cname"><span class="help">{{ catBad() ? 'Enter a category name.' : '' }}</span></div>
        <button class="btn btn-secondary" [disabled]="busy()">Add category</button></form>
    }
    @if (editing() !== null) {
      <rs-modal [label]="editing() === 'new' ? 'Add product' : 'Edit product'" (closed)="editing.set(null)">
        <h2>{{ editing() === 'new' ? 'Add product' : 'Edit product' }}</h2>
        <form [formGroup]="pf" (ngSubmit)="save()" novalidate>
          @if (formError()) { <div class="alert error" role="alert">{{ formError() }}</div> }
          <div class="field" [class.err]="bad('name')"><label for="pname">Name</label><input id="pname" formControlName="name"><span class="help">{{ bad('name') ? 'Enter a product name.' : '' }}</span></div>
          <div class="field"><label for="pdesc">Description</label><textarea id="pdesc" rows="3" formControlName="description"></textarea></div>
          <div class="field" [class.err]="bad('imageUrl')"><label for="pimg">Image URL</label><input id="pimg" type="url" formControlName="imageUrl" placeholder="https://..."><span class="help">{{ bad('imageUrl') ? 'Enter a full link starting with http:// or https://.' : '' }}</span></div>
          <div class="split">
            <div class="field" [class.err]="bad('price')"><label for="pprice">Price (₹)</label><input id="pprice" type="number" min="0" step="0.01" formControlName="price"><span class="help">{{ bad('price') ? 'Price must be greater than 0.' : '' }}</span></div>
            <div class="field" [class.err]="bad('discount')"><label for="pdisc">Discount (%)</label><input id="pdisc" type="number" min="0" max="100" formControlName="discount"><span class="help">{{ bad('discount') ? 'Discount must be between 0 and 100.' : '' }}</span></div></div>
          <div class="split">
            <div class="field" [class.err]="bad('quantity')"><label for="pqty">Stock</label><input id="pqty" type="number" min="0" formControlName="quantity"><span class="help">{{ bad('quantity') ? 'Enter the stock on hand (a whole number, 0 or more).' : '' }}</span></div>
            <div class="field" [class.err]="bad('threshold')"><label for="pth">Low-stock alert at</label><input id="pth" type="number" min="0" formControlName="threshold"><span class="help">{{ bad('threshold') ? 'Enter a whole number, 0 or more.' : '' }}</span></div></div>
          <div class="field"><label for="pcat">Category</label><select id="pcat" formControlName="category"><option [ngValue]="null">No category</option>@for (c of cats(); track c.id) { <option [ngValue]="c.id">{{ c.name }}</option> }</select></div>
          <div class="row end"><button type="button" class="btn btn-secondary" (click)="editing.set(null)">Cancel</button>
            <button class="btn btn-primary" [disabled]="busy()">{{ editing() === 'new' ? 'Add product' : 'Save changes' }}</button></div>
        </form></rs-modal> }`,
})
export class Seller {
  private api = inject(Api); private auth = inject(Auth); private ui = inject(Ui); private fb = inject(FormBuilder);
  readonly mine = signal<Product[]>([]); readonly cats = signal<Category[]>([]);
  readonly loading = signal(true); readonly error = signal<string | null>(null); readonly busy = signal(false);
  readonly editing = signal<Product | 'new' | null>(null); readonly formError = signal<string | null>(null);
  readonly low = computed(() => this.mine().filter(p => p.quantity <= (p.lowStockThreshold ?? 5)));
  private submitted = signal(false); private orderSubmitted = signal(false); private catSubmitted = signal(false);
  readonly pf = this.fb.group({
    name: ['', [Validators.required, Validators.pattern(/\S/)]], description: [''], imageUrl: ['', Validators.pattern(/^$|^https?:\/\/\S+$/i)],
    price: [null as number | null, [Validators.required, Validators.min(0.01)]], discount: [0 as number | null, [Validators.min(0), Validators.max(100)]],
    quantity: [null as number | null, [Validators.required, Validators.min(0), Validators.pattern(/^\d+$/)]],
    threshold: [5 as number | null, [Validators.min(0), Validators.pattern(/^\d+$/)]], category: [null as number | null],
  });
  readonly orderForm = this.fb.group({ oid: [null as number | null, [Validators.required, Validators.min(1)]], st: ['CONFIRMED'] });
  readonly catForm = this.fb.nonNullable.group({ cname: ['', [Validators.required, Validators.pattern(/\S/)]] });
  constructor() { void this.load(); }

  state(p: Product) { return p.quantity <= 0 ? { kind: 'bad', text: 'Out of stock' } : isLow(p) ? { kind: 'warn', text: 'Low stock' } : { kind: 'ok', text: 'Active' }; }
  bad(n: keyof typeof this.pf.controls) { const c = this.pf.controls[n]; return c.invalid && (c.touched || this.submitted()); }
  orderBad() { const c = this.orderForm.controls.oid; return c.invalid && (c.touched || this.orderSubmitted()); }
  catBad() { const c = this.catForm.controls.cname; return c.invalid && (c.touched || this.catSubmitted()); }

  async load() {
    this.error.set(null);
    try {
      const [ps, cs] = await Promise.all([firstValueFrom(this.api.products()), firstValueFrom(this.api.categories())]);
      this.mine.set(ps.filter(p => p.sellerId === this.auth.userId())); this.cats.set(cs);
    } catch (e) { this.error.set(friendly(e, 'We couldn’t load your dashboard — try again.')); }
    finally { this.loading.set(false); }
  }
  openForm(p: Product | null) {
    this.submitted.set(false); this.formError.set(null);
    this.pf.reset(p ? { name: p.name, description: p.description ?? '', imageUrl: p.imageUrl ?? '', price: p.price, discount: p.discount ?? 0, quantity: p.quantity, threshold: p.lowStockThreshold ?? 5, category: p.category?.id ?? null }
      : { name: '', description: '', imageUrl: '', price: null, discount: 0, quantity: null, threshold: 5, category: null });
    this.editing.set(p ?? 'new');
  }
  async save() {
    this.submitted.set(true); this.formError.set(null);
    if (this.pf.invalid || this.busy()) { this.pf.markAllAsTouched(); return; }
    const v = this.pf.getRawValue(); const e = this.editing();
    const body: ProductPayload = {
      name: (v.name ?? '').trim(), description: (v.description ?? '').trim(), imageUrl: (v.imageUrl ?? '').trim() || null, price: Number(v.price),
      discount: Number(v.discount ?? 0), quantity: Number(v.quantity), lowStockThreshold: Number(v.threshold ?? 0), category: v.category != null ? { id: v.category } : null,
    };
    this.busy.set(true);
    try {
      await firstValueFrom(e && e !== 'new' ? this.api.updateProduct(e.id, body) : this.api.createProduct(body));
      this.editing.set(null); this.ui.show(e === 'new' ? 'Product added.' : 'Changes saved.'); await this.load();
    } catch (err) { this.formError.set(friendly(err, 'We couldn’t save this product. Check the details and try again.')); }
    finally { this.busy.set(false); }
  }
  async remove(p: Product) {
    if (!(await this.ui.confirm(`Delete “${p.name}”? This can’t be undone.`, 'Delete product'))) return;
    try { await firstValueFrom(this.api.deleteProduct(p.id)); this.ui.show('Product deleted.'); await this.load(); }
    catch (e) { this.ui.error(e, 'We couldn’t delete this product — try again.'); }
  }
  async updateOrder() {
    this.orderSubmitted.set(true);
    if (this.orderForm.invalid || this.busy()) return;
    const { oid, st } = this.orderForm.getRawValue(); this.busy.set(true);
    try { await firstValueFrom(this.api.setStatus(Number(oid), st ?? 'CONFIRMED')); this.ui.show('Order status updated.'); this.orderForm.reset({ oid: null, st: 'CONFIRMED' }); this.orderSubmitted.set(false); }
    catch (e) { this.ui.error(e, 'We couldn’t update that order. Statuses move one step at a time: Placed → Confirmed → Shipped → Delivered.', { 404: 'We couldn’t find an order with that number.' }); }
    finally { this.busy.set(false); }
  }
  async addCategory() {
    this.catSubmitted.set(true);
    if (this.catForm.invalid || this.busy()) return;
    this.busy.set(true);
    try { await firstValueFrom(this.api.createCategory(this.catForm.getRawValue().cname.trim())); this.ui.show('Category added.'); this.catForm.reset(); this.catSubmitted.set(false); this.cats.set(await firstValueFrom(this.api.categories())); }
    catch (e) { this.ui.error(e, 'We couldn’t add that category — it may already exist.'); }
    finally { this.busy.set(false); }
  }
}
