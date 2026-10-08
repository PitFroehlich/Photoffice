import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { authProvider, fakeAuthService } from '../../testing/test-providers';
import { platformGuard } from './platform.guard';
import { studioGuard } from './studio.guard';

describe('area guards', () => {
  let auth: ReturnType<typeof fakeAuthService>;

  beforeEach(() => {
    auth = fakeAuthService();
    TestBed.configureTestingModule({ providers: [provideRouter([]), authProvider(auth)] });
  });

  const run = (guard: typeof studioGuard, url: string) =>
    TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    );

  const redirect = (result: unknown) => (result instanceof UrlTree ? result.toString() : result);

  describe('studioGuard', () => {
    it('lets logged-in studio users pass', () => {
      auth.state.set(true);

      expect(run(studioGuard, '/studio')).toBe(true);
      expect(auth.login).not.toHaveBeenCalled();
    });

    it('sends anonymous users to the login and remembers the target', () => {
      expect(run(studioGuard, '/studio')).toBe(false);
      expect(auth.login).toHaveBeenCalledWith('/studio');
    });

    it('leads the platform operator to the platform area', () => {
      auth.state.set(true);
      auth.roleState.set(['platform-admin']);

      expect(redirect(run(studioGuard, '/studio'))).toBe('/plattform');
    });
  });

  describe('platformGuard', () => {
    it('lets the platform operator pass', () => {
      auth.state.set(true);
      auth.roleState.set(['platform-admin']);

      expect(run(platformGuard, '/plattform')).toBe(true);
    });

    it('sends anonymous users to the login and remembers the target', () => {
      expect(run(platformGuard, '/plattform/studios')).toBe(false);
      expect(auth.login).toHaveBeenCalledWith('/plattform/studios');
    });

    it('leads studio users to the studio area', () => {
      auth.state.set(true);

      expect(redirect(run(platformGuard, '/plattform'))).toBe('/studio');
    });
  });
});
