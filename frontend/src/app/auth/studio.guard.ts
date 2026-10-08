import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

/** Only logged-in users enter the studio area; everybody else is sent to the login page. */
export const studioGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.hasValidAccessToken()) {
    return true;
  }
  auth.login(state.url);
  return false;
};
