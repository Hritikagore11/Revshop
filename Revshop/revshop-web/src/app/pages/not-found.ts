import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'rs-not-found', imports: [RouterLink], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="empty"><p>We couldn’t find that page.</p><a class="btn btn-primary" routerLink="/">Go home</a></div>`,
})
export class NotFound {}
