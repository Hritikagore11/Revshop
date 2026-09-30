import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { Auth } from './auth.service';

const PUBLIC = /\/users\/(login|register)$/;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(Auth);
  const token = auth.token();
  const isPublic = PUBLIC.test(req.url.split('?')[0]);
  const outgoing = token && !isPublic ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(outgoing).pipe(
    catchError((e: unknown) => {
      // Spring answers 401/403 for a missing or expired token. A 403 with a valid token just means "wrong role", so keep the session.
      if (e instanceof HttpErrorResponse && token && !isPublic && (e.status === 401 || (e.status === 403 && !auth.isValid()))) {
        auth.logout('Session expired. Log in again to continue.');
      }
      return throwError(() => e);
    }),
  );
};
