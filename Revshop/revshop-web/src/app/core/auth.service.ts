import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Api } from './api.service';
import { Claims, Profile, Role } from './models';

const KEY = 'rs_token';

function decode(token: string | null): Claims | null {
  if (!token) return null;
  try {
    const b64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const c = JSON.parse(atob(b64.padEnd(b64.length + ((4 - (b64.length % 4)) % 4), '=')));
    if (!c.exp || c.exp * 1000 <= Date.now()) return null;
    return { sub: c.sub, userId: Number(c.userId), exp: c.exp, role: String(c.role ?? '').trim().toUpperCase().replace(/^ROLE_/, '') as Role };
  } catch { return null; }
}
const read = () => { try { return localStorage.getItem(KEY); } catch { return null; } };

@Injectable({ providedIn: 'root' })
export class Auth {
  private api = inject(Api);
  private router = inject(Router);
  readonly token = signal<string | null>(read());
  readonly profile = signal<Profile | null>(null);
  readonly flash = signal<string | null>(null);
  readonly claims = computed(() => decode(this.token()));
  readonly loggedIn = computed(() => !!this.claims());
  readonly role = computed(() => this.claims()?.role ?? null);
  readonly userId = computed(() => this.claims()?.userId ?? 0);
  readonly isBuyer = computed(() => this.role() === 'BUYER');
  readonly isSeller = computed(() => this.role() === 'SELLER');
  readonly name = computed(() => this.profile()?.name || this.claims()?.sub || '');

  /** Re-checks expiry against the clock (computed signals don't tick). */
  isValid() { return !!decode(this.token()); }

  async login(email: string, password: string) {
    const t = (await firstValueFrom(this.api.login(email, password))).trim();
    if (!decode(t)) throw new Error('bad-token');
    try { localStorage.setItem(KEY, t); } catch { /* private mode: session-only */ }
    this.token.set(t);
    await this.loadProfile();
  }
  async loadProfile() { try { this.profile.set(await firstValueFrom(this.api.profile())); } catch { /* falls back to email */ } }
  clear() { try { localStorage.removeItem(KEY); } catch { /* ignore */ } this.token.set(null); this.profile.set(null); }
  logout(message?: string) { this.clear(); this.flash.set(message ?? null); void this.router.navigate(['/login']); }
}
