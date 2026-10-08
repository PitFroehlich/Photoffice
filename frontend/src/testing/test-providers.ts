import { computed, signal } from '@angular/core';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { AuthService, PLATFORM_ADMIN, STUDIO_ROLES } from '../app/auth/auth.service';
import { StudioSession } from '../app/studio/studio-session';

/** Icons without HTTP requests in unit tests. Add to `imports` of the testing module. */
export const iconTesting = MatIconTestingModule;

/** AuthService double with controllable login state and realm roles (e.g. ['platform-admin']). */
export function fakeAuthService(loggedIn = false, roles: string[] = ['studio-admin']) {
  const state = signal(loggedIn);
  const roleState = signal(roles);
  return {
    state,
    roleState,
    isLoggedIn: state.asReadonly(),
    roles: roleState.asReadonly(),
    isPlatformAdmin: computed(() => roleState().includes(PLATFORM_ADMIN)),
    isStudioUser: computed(() => roleState().some((role) => STUDIO_ROLES.includes(role))),
    displayName: signal('Paula Plattform').asReadonly(),
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
