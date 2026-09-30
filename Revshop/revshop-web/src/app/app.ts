import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { Auth } from './core/auth.service';
import { Badges } from './core/badges.service';
import { Ui } from './core/ui.service';
import { Modal } from './shared/shared';

@Component({
  selector: 'app-root', imports: [RouterOutlet, RouterLink, RouterLinkActive, Modal], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<a class="skip" href="#main" (click)="focusMain($event)">Skip to content</a>
    <header class="topbar"><div class="topbar-inner">
      <a class="logo" routerLink="/">RevShop</a>
      @if (auth.loggedIn()) {
        <nav class="tabs" aria-label="Main">
          <a routerLink="/" routerLinkActive ariaCurrentWhenActive="page" [routerLinkActiveOptions]="{ exact: true }">Home</a>
          <a routerLink="/products" routerLinkActive ariaCurrentWhenActive="page">Products</a>
          @if (auth.isBuyer()) {
            <a routerLink="/cart" routerLinkActive ariaCurrentWhenActive="page">Cart @if (badges.cart()) { <span class="count" [attr.aria-label]="badges.cart() + ' items'">{{ badges.cart() }}</span> }</a>
            <a routerLink="/orders" routerLinkActive ariaCurrentWhenActive="page">Orders</a>
          } @else { <a routerLink="/seller" routerLinkActive ariaCurrentWhenActive="page">Seller dashboard</a> }
          <a routerLink="/notifications" routerLinkActive ariaCurrentWhenActive="page">Notifications @if (badges.unread()) { <span class="pip" role="img" aria-label="Unread notifications"></span> }</a>
        </nav>
        <div class="user"><span>{{ auth.name() }}</span><span class="badge" [class]="auth.isBuyer() ? 'buyer' : 'seller'">{{ auth.isBuyer() ? 'Buyer' : 'Seller' }}</span>
          <button class="btn btn-secondary btn-sm" (click)="auth.logout()">Log out</button></div>
      }
    </div></header>
    <main id="main" tabindex="-1"><router-outlet /></main>
    <div id="toast" aria-live="polite">@if (ui.toast(); as t) { <div class="alert" [class]="t.kind" [attr.role]="t.kind === 'error' ? 'alert' : 'status'">{{ t.text }}</div> }</div>
    @if (ui.confirmState(); as c) {
      <rs-modal label="Confirm" (closed)="ui.answer(false)"><h3 style="margin-top:0">{{ c.text }}</h3>
        <div class="row end"><button class="btn btn-secondary" (click)="ui.answer(false)">Keep</button><button class="btn btn-danger" (click)="ui.answer(true)">{{ c.label }}</button></div></rs-modal> }
    <footer>RevShop</footer>`,
})
export class App {
  readonly auth = inject(Auth); readonly ui = inject(Ui); readonly badges = inject(Badges);
  constructor() {
    inject(Router).events.pipe(filter(e => e instanceof NavigationEnd), takeUntilDestroyed()).subscribe(() => {
      void this.badges.refresh(); document.getElementById('main')?.focus({ preventScroll: true });
    });
  }
  focusMain(e: Event) { e.preventDefault(); document.getElementById('main')?.focus(); }
}
