import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Api } from '../core/api.service';
import { Auth } from '../core/auth.service';
import { Badges } from '../core/badges.service';
import { friendly } from '../core/errors';
import { AppNotification } from '../core/models';
import { Ui } from '../core/ui.service';
import { PIPES, byNewest } from '../core/util';

@Component({
  selector: 'rs-notifications', imports: [...PIPES], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<h1>Notifications</h1>
    @if (loading()) { <p class="muted" role="status">Loading…</p> }
    @else if (error()) { <div class="alert error" role="alert">{{ error() }} <button class="btn btn-secondary btn-sm" (click)="load()">Retry</button></div> }
    @else {
      @if (items().length) { <div class="row end" style="margin:0 0 8px"><button class="btn btn-secondary btn-sm" (click)="markAll()" [disabled]="!hasUnread() || busy()">Mark all as read</button></div> }
      @for (n of items(); track n.id) {
        @if (!isRead(n) && auth.isBuyer()) {
          <button type="button" class="notif" (click)="markOne(n)" title="Mark as read"><span class="dot"></span><span><span class="t" style="display:block">{{ n.message }}</span><span class="m" style="display:block">{{ n.createdAt | ago }}</span></span></button>
        } @else {
          <div class="notif"><span class="dot" [class.none]="isRead(n)"></span><span><span class="t" style="display:block">{{ n.message }}</span><span class="m" style="display:block">{{ n.createdAt | ago }}</span></span></div>
        }
      } @empty { <div class="empty"><p>You’re all caught up — order and stock updates will show up here.</p></div> } }`,
})
export class Notifications {
  private api = inject(Api); private ui = inject(Ui); private badges = inject(Badges); readonly auth = inject(Auth);
  readonly items = signal<AppNotification[]>([]); readonly loading = signal(true); readonly busy = signal(false); readonly error = signal<string | null>(null);
  readonly hasUnread = computed(() => this.items().some(n => !this.isRead(n)));
  constructor() { void this.load(); }
  isRead(n: AppNotification) { return n.isRead ?? n.read ?? false; }
  async load() {
    this.error.set(null); const uid = this.auth.userId();
    try {
      let ns: AppNotification[];
      if (this.auth.isBuyer()) ns = await firstValueFrom(this.api.notifications(uid));
      else { // the full list is buyer-only on the backend, so sellers see unread + low-stock alerts
        const [a, b] = await Promise.all([firstValueFrom(this.api.unread(uid)), firstValueFrom(this.api.lowStock(uid))]);
        ns = [...new Map([...a, ...b].map(n => [n.id, n])).values()];
      }
      this.items.set(ns.sort(byNewest)); void this.badges.refresh();
    } catch (e) { this.error.set(friendly(e, 'We couldn’t load notifications — try again.')); }
    finally { this.loading.set(false); }
  }
  async markOne(n: AppNotification) { await this.run(() => firstValueFrom(this.api.markRead(n.id))); }
  async markAll() { await this.run(() => firstValueFrom(this.api.readAll(this.auth.userId()))); }
  private async run(fn: () => Promise<unknown>) {
    this.busy.set(true);
    try { await fn(); await this.load(); } catch (e) { this.ui.error(e, 'We couldn’t update your notifications — try again.'); } finally { this.busy.set(false); }
  }
}
