import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Api } from './api.service';
import { Auth } from './auth.service';

/** Header counters (cart lines, unread notifications). Failures are silent: they're decoration, not data. */
@Injectable({ providedIn: 'root' })
export class Badges {
  private api = inject(Api);
  private auth = inject(Auth);
  readonly cart = signal(0);
  readonly unread = signal(false);

  async refresh() {
    if (!this.auth.loggedIn()) { this.cart.set(0); this.unread.set(false); return; }
    const uid = this.auth.userId();
    await Promise.allSettled([
      this.auth.isBuyer() ? firstValueFrom(this.api.cart()).then(c => this.cart.set(c.length)) : Promise.resolve(),
      firstValueFrom(this.api.unread(uid)).then(n => this.unread.set(n.length > 0)),
    ]);
  }
}
