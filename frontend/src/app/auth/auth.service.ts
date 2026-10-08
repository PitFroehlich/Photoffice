import { Injectable, computed, inject, signal } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './auth.config';

/** Keycloak realm roles (realm_access.roles in the access token). */
export const PLATFORM_ADMIN = 'platform-admin';
export const STUDIO_ROLES = ['studio-admin', 'photographer'];

interface AccessTokenClaims {
  name?: string;
  preferred_username?: string;
  realm_access?: { roles?: string[] };
}

/**
 * Wraps the OIDC client: login state, roles and name of the logged-in user, login and logout.
 * Roles only decide where the UI leads the user – the backend checks them on every call.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauthService = inject(OAuthService);

  private readonly loggedIn = signal(false);
  private readonly claims = signal<AccessTokenClaims>({});
  readonly isLoggedIn = this.loggedIn.asReadonly();

  readonly roles = computed(() => this.claims().realm_access?.roles ?? []);
  readonly isPlatformAdmin = computed(() => this.roles().includes(PLATFORM_ADMIN));
  readonly isStudioUser = computed(() => this.roles().some((role) => STUDIO_ROLES.includes(role)));
  readonly displayName = computed(
    () => this.claims().name || this.claims().preferred_username || '',
  );

  /** Called once at application start; completes a running login (code in URL) if present. */
  async init(): Promise<void> {
    this.oauthService.configure(authConfig);
    this.oauthService.events.subscribe(() => this.updateState());
    try {
      await this.oauthService.loadDiscoveryDocumentAndTryLogin();
      this.oauthService.setupAutomaticSilentRefresh();
    } catch (error) {
      // Login server not reachable: the public part of the app keeps working
      console.error('OIDC initialisation failed', error);
    }
    this.updateState();
  }

  hasValidAccessToken(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  login(targetUrl = '/studio'): void {
    this.oauthService.initCodeFlow(targetUrl);
  }

  logout(): void {
    this.oauthService.logOut();
    this.loggedIn.set(false);
    this.claims.set({});
  }

  private updateState(): void {
    const valid = this.oauthService.hasValidAccessToken();
    this.loggedIn.set(valid);
    this.claims.set(valid ? decodeClaims(this.oauthService.getAccessToken()) : {});
  }
}

/** Payload of a JWT (no signature check – only for UI decisions). */
export function decodeClaims(token: string | null | undefined): AccessTokenClaims {
  const payload = token?.split('.')[1];
  if (!payload) {
    return {};
  }
  try {
    const base64 = payload
      .replace(/-/g, '+')
      .replace(/_/g, '/')
      .padEnd(Math.ceil(payload.length / 4) * 4, '=');
    const bytes = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes)) as AccessTokenClaims;
  } catch {
    return {};
  }
}
