import { signal } from '@angular/core';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { AuthService } from '../app/auth/auth.service';

/** Icons without HTTP requests in unit tests. Add to `imports` of the testing module. */
export const iconTesting = MatIconTestingModule;

/** AuthService double with controllable login state. */
export function fakeAuthService(loggedIn = false) {
  const state = signal(loggedIn);
  return {
    state,
    isLoggedIn: state.asReadonly(),
    hasValidAccessToken: () => state(),
    login: vi.fn(),
    logout: vi.fn(),
  };
}

export const authProvider = (auth: ReturnType<typeof fakeAuthService>) => ({ provide: AuthService, useValue: auth });
