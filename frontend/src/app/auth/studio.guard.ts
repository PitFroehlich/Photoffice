import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

/**
 * Only logged-in users enter the studio area; everybody else is sent to the login page.
 * The platform operator (no studio role) is led to the platform area – after login everybody lands on /studio.
 */
export const studioGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (!auth.hasValidAccessToken()) {
    auth.login(state.url);
    return false;
  }
  if (auth.isPlatformAdmin() && !auth.isStudioUser()) {
    return inject(Router).createUrlTree(['/plattform']);
  }
  return true;
};
