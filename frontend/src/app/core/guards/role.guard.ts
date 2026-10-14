import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { Role } from '../models/auth.model';

/** À utiliser avec `data: { roles: [...], teamLeaders?: true }` sur la route. */
export const roleGuard: CanActivateFn = (route) => {
  const roles = (route.data?.['roles'] ?? []) as Role[];
  const auth = inject(AuthService);
  if (auth.canAccess(roles, route.data?.['teamLeaders'] === true)) return true;
  return inject(Router).createUrlTree(['/dashboard']);
};
