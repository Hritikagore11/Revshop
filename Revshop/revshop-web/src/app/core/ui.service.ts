import { Injectable, signal } from '@angular/core';
import { friendly } from './errors';

@Injectable({ providedIn: 'root' })
export class Ui {
  readonly toast = signal<{ text: string; kind: 'ok' | 'error' | 'warn' } | null>(null);
  readonly confirmState = signal<{ text: string; label: string; resolve: (v: boolean) => void } | null>(null);
  private timer?: number;

  show(text: string, kind: 'ok' | 'error' | 'warn' = 'ok') {
    this.toast.set({ text, kind });
    clearTimeout(this.timer);
    this.timer = window.setTimeout(() => this.toast.set(null), 4500);
  }
  error(e: unknown, fallback?: string, byStatus?: Record<number, string>) { this.show(friendly(e, fallback, byStatus), 'error'); }
  confirm(text: string, label: string) { return new Promise<boolean>(resolve => this.confirmState.set({ text, label, resolve })); }
  answer(v: boolean) { const s = this.confirmState(); this.confirmState.set(null); s?.resolve(v); }
}
