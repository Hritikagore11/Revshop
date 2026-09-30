import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from './auth.service';
import { Role } from './models';

export const authGuard: CanActivateFn = (_r, state) => {
  const auth = inject(Auth);
  if (auth.isValid()) return true;
  if (auth.token()) { auth.clear(); auth.flash.set('Session expired. Log in again to continue.'); }
  return inject(Router).createUrlTree(['/login'], { queryParams: state.url === '/' ? {} : { returnUrl: state.url } });
};
export const guestGuard: CanActivateFn = () => (inject(Auth).isValid() ? inject(Router).createUrlTree(['/']) : true);
export const roleGuard = (role: Role): CanActivateFn => () => {
  const auth = inject(Auth);
  return auth.role() === role ? true : inject(Router).createUrlTree([auth.role() === 'SELLER' ? '/seller' : '/']);
};
