import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Api } from '../core/api.service';
import { Auth } from '../core/auth.service';
import { friendly } from '../core/errors';
import { Ui } from '../core/ui.service';

const safeReturn = (u: string | null) => (u && u.startsWith('/') && !u.startsWith('//') ? u : null);

@Component({
  selector: 'rs-login', imports: [ReactiveFormsModule, RouterLink], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="auth"><h1>Log in</h1>
    @if (flash) { <div class="alert error" role="alert">{{ flash }}</div> }
    @if (error()) { <div class="alert error" role="alert">{{ error() }}</div> }
    <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <div class="field" [class.err]="bad('email')"><label for="email">Email</label>
        <input id="email" type="email" formControlName="email" autocomplete="username" [attr.aria-invalid]="bad('email')">
        <span class="help">{{ bad('email') ? 'Enter a valid email like you&#64;example.com.' : '' }}</span></div>
      <div class="field" [class.err]="bad('password')"><label for="password">Password</label>
        <input id="password" type="password" formControlName="password" autocomplete="current-password" [attr.aria-invalid]="bad('password')">
        <span class="help">{{ bad('password') ? 'Enter your password.' : '' }}</span></div>
      <button class="btn btn-primary btn-block" [disabled]="busy()">{{ busy() ? 'Logging in…' : 'Log in' }}</button>
    </form>
    <p class="muted">New to RevShop? <a routerLink="/register">Create an account</a></p></div>`,
})
export class Login {
  private fb = inject(FormBuilder); private auth = inject(Auth); private router = inject(Router); private route = inject(ActivatedRoute);
  readonly flash = this.auth.flash();
  readonly form = this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]], password: ['', Validators.required] });
  readonly submitted = signal(false); readonly busy = signal(false); readonly error = signal<string | null>(null);
  constructor() { this.auth.flash.set(null); }
  bad(n: 'email' | 'password') { const c = this.form.controls[n]; return c.invalid && (c.touched || this.submitted()); }
  async submit() {
    this.submitted.set(true); this.error.set(null);
    if (this.form.invalid || this.busy()) return;
    this.busy.set(true);
    try {
      const { email, password } = this.form.getRawValue();
      await this.auth.login(email.trim(), password);
      await this.router.navigateByUrl(safeReturn(this.route.snapshot.queryParamMap.get('returnUrl')) ?? (this.auth.isSeller() ? '/seller' : '/'));
    } catch (e) {
      this.error.set(friendly(e, 'We couldn’t log you in. Try again.', { 401: 'Email or password is incorrect. Check them and try again.', 403: 'Email or password is incorrect. Check them and try again.' }));
    } finally { this.busy.set(false); }
  }
}

@Component({
  selector: 'rs-register', imports: [ReactiveFormsModule, RouterLink], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="auth"><h1>Create account</h1>
    @if (error()) { <div class="alert error" role="alert">{{ error() }}</div> }
    <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <div class="field" [class.err]="bad('name')"><label for="name">Full name</label><input id="name" formControlName="name" autocomplete="name">
        <span class="help">{{ bad('name') ? 'Enter your full name.' : '' }}</span></div>
      <div class="field" [class.err]="bad('email')"><label for="email">Email</label><input id="email" type="email" formControlName="email" autocomplete="username">
        <span class="help">{{ bad('email') ? 'Enter a valid email like you&#64;example.com.' : '' }}</span></div>
      <div class="field" [class.err]="bad('password')"><label for="password">Password</label><input id="password" type="password" formControlName="password" autocomplete="new-password">
        <span class="help">{{ bad('password') ? 'Must be at least 6 characters.' : '' }}</span></div>
      <div class="field" [class.err]="bad('phone')"><label for="phone">Phone (optional)</label><input id="phone" type="tel" formControlName="phone" autocomplete="tel">
        <span class="help">{{ bad('phone') ? 'Enter a valid phone number.' : '' }}</span></div>
      <div class="field"><label for="role">I want to</label><select id="role" formControlName="role"><option value="BUYER">Buy products</option><option value="SELLER">Sell products</option></select></div>
      <button class="btn btn-primary btn-block" [disabled]="busy()">{{ busy() ? 'Creating account…' : 'Create account' }}</button>
    </form>
    <p class="muted">Already registered? <a routerLink="/login">Log in</a></p></div>`,
})
export class Register {
  private fb = inject(FormBuilder); private api = inject(Api); private auth = inject(Auth); private router = inject(Router); private ui = inject(Ui);
  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.pattern(/\S/)]], email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]], phone: ['', Validators.pattern(/^$|^[+\d][\d\s-]{6,14}$/)], role: ['BUYER'],
  });
  readonly submitted = signal(false); readonly busy = signal(false); readonly error = signal<string | null>(null);
  bad(n: 'name' | 'email' | 'password' | 'phone') { const c = this.form.controls[n]; return c.invalid && (c.touched || this.submitted()); }
  async submit() {
    this.submitted.set(true); this.error.set(null);
    if (this.form.invalid || this.busy()) return;
    this.busy.set(true);
    const v = this.form.getRawValue(); const email = v.email.trim();
    try {
      await firstValueFrom(this.api.register({ name: v.name.trim(), email, password: v.password, phone: v.phone.trim(), role: v.role }));
    } catch (e) {
      this.error.set(friendly(e, 'We couldn’t create your account. Check your details and try again.', { 409: 'That email is already registered. Log in instead.' }));
      this.busy.set(false); return;
    }
    try {
      await this.auth.login(email, v.password);
      this.ui.show('Account created. Welcome to RevShop.');
      await this.router.navigateByUrl(this.auth.isSeller() ? '/seller' : '/');
    } catch {
      this.ui.show('Account created. Log in to continue.');
      await this.router.navigate(['/login']);
    } finally { this.busy.set(false); }
  }
}
