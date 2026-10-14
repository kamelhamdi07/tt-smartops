import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';

/** Ajoute le jeton JWT, ramène à la connexion si la session a expiré et resynchronise le rôle sur un 403. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const request = token && req.url.startsWith('/api/')
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(request).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !req.url.endsWith('/auth/login')) {
        auth.logout();
      } else if (err.status === 403 && !req.url.endsWith('/auth/me')) {
        // rôle modifié par l'ADMIN depuis la connexion : on resynchronise la session
        auth.syncUser();
      }
      return throwError(() => err);
    })
  );
};
