import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

/**
 * Platform area: only for the platform operator (realm role platform-admin). Anonymous users are sent to the login;
 * other users are led to the studio area.
 */
export const platformGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (!auth.hasValidAccessToken()) {
    auth.login(state.url);
    return false;
  }
  return auth.isPlatformAdmin() ? true : inject(Router).createUrlTree(['/studio']);
};
