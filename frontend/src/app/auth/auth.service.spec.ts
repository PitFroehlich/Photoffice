import { TestBed } from '@angular/core/testing';
import { OAuthService } from 'angular-oauth2-oidc';
import { Subject } from 'rxjs';
import { AuthService, accountActionResultFrom, decodeClaims, safeTargetUrl } from './auth.service';

/** Builds an unsigned JWT with the given payload (base64url, UTF-8). */
function jwt(payload: object): string {
  const bytes = new TextEncoder().encode(JSON.stringify(payload));
  const base64 = btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return `header.${base64}.signature`;
}

describe('decodeClaims', () => {
  it('reads realm roles and name from the access token', () => {
    const claims = decodeClaims(
      jwt({
        name: 'Jürgen Größe',
        preferred_username: 'operator',
        realm_access: { roles: ['platform-admin'] },
      }),
    );

    expect(claims.name).toBe('Jürgen Größe');
    expect(claims.realm_access?.roles).toEqual(['platform-admin']);
  });

  it('returns no claims for missing or broken tokens', () => {
    expect(decodeClaims(null)).toEqual({});
    expect(decodeClaims('no-jwt')).toEqual({});
    expect(decodeClaims('a.%%%.c')).toEqual({});
  });
});

describe('AuthService account actions', () => {
  function setup(state = '') {
    const oauth = {
      state,
      events: new Subject<unknown>(),
      configure: vi.fn(),
      loadDiscoveryDocumentAndTryLogin: vi.fn().mockResolvedValue(true),
      setupAutomaticSilentRefresh: vi.fn(),
      hasValidAccessToken: () => true,
      getAccessToken: () => jwt({ name: 'Anna Neuname' }),
      initCodeFlow: vi.fn(),
    };
    TestBed.configureTestingModule({ providers: [{ provide: OAuthService, useValue: oauth }] });
    return { oauth, auth: TestBed.inject(AuthService) };
  }

  afterEach(() => history.replaceState(null, '', '/'));

  it('opens the Keycloak page for the action and remembers where to return', () => {
    const { oauth, auth } = setup();

    auth.startAccountAction('UPDATE_PASSWORD', '/studio/kunden?suche=a');

    expect(oauth.initCodeFlow).toHaveBeenCalledWith('/studio/kunden?suche=a', {
      kc_action: 'UPDATE_PASSWORD',
    });
  });

  it('returns to the start page of the action and reports the result once', async () => {
    history.replaceState(
      null,
      '',
      '/studio?iss=x&kc_action=UPDATE_PROFILE&kc_action_status=success',
    );
    const { auth } = setup('%2Fstudio%2Fkunden');

    await auth.init();

    expect(window.location.pathname).toBe('/studio/kunden');
    expect(window.location.search).toBe('');
    expect(auth.displayName()).toBe('Anna Neuname');
    expect(auth.takeAccountActionResult()).toEqual({ action: 'UPDATE_PROFILE', status: 'success' });
    expect(auth.takeAccountActionResult()).toBeUndefined();
  });

  it('keeps the URL after a normal login', async () => {
    history.replaceState(null, '', '/studio');
    const { auth } = setup('%2Fplattform');

    await auth.init();

    expect(window.location.pathname).toBe('/studio');
    expect(auth.takeAccountActionResult()).toBeUndefined();
  });
});

describe('accountActionResultFrom', () => {
  it('reads action and status that Keycloak appends', () => {
    expect(
      accountActionResultFrom('?kc_action=UPDATE_PASSWORD&kc_action_status=cancelled'),
    ).toEqual({
      action: 'UPDATE_PASSWORD',
      status: 'cancelled',
    });
  });

  it('ignores unknown or missing values', () => {
    expect(accountActionResultFrom('')).toBeUndefined();
    expect(
      accountActionResultFrom('?kc_action=delete_account&kc_action_status=success'),
    ).toBeUndefined();
    expect(accountActionResultFrom('?kc_action=UPDATE_PASSWORD')).toBeUndefined();
  });
});

describe('safeTargetUrl', () => {
  it('accepts paths inside the app', () => {
    expect(safeTargetUrl('/plattform/studios')).toBe('/plattform/studios');
    expect(safeTargetUrl('%2Fstudio%3Fa%3D1')).toBe('/studio?a=1');
  });

  it('rejects everything else', () => {
    expect(safeTargetUrl(undefined)).toBeUndefined();
    expect(safeTargetUrl('')).toBeUndefined();
    expect(safeTargetUrl('https://evil.test')).toBeUndefined();
    expect(safeTargetUrl('//evil.test')).toBeUndefined();
    expect(safeTargetUrl('/\\evil.test')).toBeUndefined();
    expect(safeTargetUrl('%E0%A4%A')).toBeUndefined();
  });
});
