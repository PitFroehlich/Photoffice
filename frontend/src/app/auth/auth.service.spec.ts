import { decodeClaims } from './auth.service';

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
