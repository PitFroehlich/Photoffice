import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';
import { studioGuard } from './studio.guard';

describe('studioGuard', () => {
  const login = vi.fn();
  let loggedIn = false;

  beforeEach(() => {
    login.mockReset();
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: { hasValidAccessToken: () => loggedIn, login } }],
    });
  });

  const runGuard = () =>
    TestBed.runInInjectionContext(() =>
      studioGuard({} as ActivatedRouteSnapshot, { url: '/studio' } as RouterStateSnapshot),
    );

  it('lets logged-in users pass', () => {
    loggedIn = true;

    expect(runGuard()).toBe(true);
    expect(login).not.toHaveBeenCalled();
  });

  it('sends anonymous users to the login and remembers the target', () => {
    loggedIn = false;

    expect(runGuard()).toBe(false);
    expect(login).toHaveBeenCalledWith('/studio');
  });
});
