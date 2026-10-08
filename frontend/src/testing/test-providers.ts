import { signal } from '@angular/core';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { AuthService } from '../app/auth/auth.service';
import { StudioSession } from '../app/studio/studio-session';

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

export const authProvider = (auth: ReturnType<typeof fakeAuthService>) => ({
  provide: AuthService,
  useValue: auth,
});

/** StudioSession double: logged in as a user of "Studio A" with the given role. */
export function studioSessionAs(role: 'STUDIO_ADMIN' | 'PHOTOGRAPHER') {
  const user = signal({
    username: role === 'STUDIO_ADMIN' ? 'admin-a' : 'foto-a',
    roles: [role],
    studio: { id: 'a', slug: 'studio-a', name: 'Studio A' },
  });
  return {
    provide: StudioSession,
    useValue: { user: user.asReadonly(), status: signal('ready').asReadonly() },
  };
}
