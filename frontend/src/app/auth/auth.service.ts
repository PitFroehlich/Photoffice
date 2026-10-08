import { Injectable, computed, inject, signal } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './auth.config';

/** Keycloak realm roles (realm_access.roles in the access token). */
export const PLATFORM_ADMIN = 'platform-admin';
export const STUDIO_ROLES = ['studio-admin', 'photographer'];

/** Keycloak application-initiated actions offered in the user menu (issue #46). */
export type AccountAction = 'UPDATE_PASSWORD' | 'UPDATE_PROFILE';

/** Outcome of an account action, reported by Keycloak as kc_action_status on the way back. */
export interface AccountActionResult {
  action: AccountAction;
  status: 'success' | 'cancelled' | 'error';
}

interface AccessTokenClaims {
  name?: string;
  preferred_username?: string;
  realm_access?: { roles?: string[] };
}

/**
 * Wraps the OIDC client: login state, roles and name of the logged-in user, login and logout, and the account actions
 * (change password, edit profile) that replace the Keycloak account console (ADR 0013).
 * Roles only decide where the UI leads the user – the backend checks them on every call.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauthService = inject(OAuthService);

  private readonly loggedIn = signal(false);
  private readonly claims = signal<AccessTokenClaims>({});
  private accountActionResult: AccountActionResult | undefined;
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
    this.accountActionResult = accountActionResultFrom(window.location.search);
    try {
      await this.oauthService.loadDiscoveryDocumentAndTryLogin();
      // Back from an account action: continue on the page it was started from (redirect URI is always /studio)
      const target = safeTargetUrl(this.oauthService.state);
      if (target && this.accountActionResult) {
        history.replaceState(null, '', target);
      }
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

  /**
   * Opens the Keycloak page for the action (application-initiated action, kc_action). Keycloak returns to the app –
   * also on cancel – with a new token (e.g. with the changed name); the app then shows returnUrl again.
   */
  startAccountAction(action: AccountAction, returnUrl: string): void {
    this.oauthService.initCodeFlow(returnUrl, { kc_action: action });
  }

  /** Result of the account action the user just returned from (only once, for a confirmation message). */
  takeAccountActionResult(): AccountActionResult | undefined {
    const result = this.accountActionResult;
    this.accountActionResult = undefined;
    return result;
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

const accountActions: AccountAction[] = ['UPDATE_PASSWORD', 'UPDATE_PROFILE'];
const accountActionStatuses: AccountActionResult['status'][] = ['success', 'cancelled', 'error'];

/** Reads kc_action/kc_action_status that Keycloak appends when returning from an account action. */
export function accountActionResultFrom(search: string): AccountActionResult | undefined {
  const params = new URLSearchParams(search);
  const action = params.get('kc_action') as AccountAction;
  const status = params.get('kc_action_status') as AccountActionResult['status'];
  return accountActions.includes(action) && accountActionStatuses.includes(status)
    ? { action, status }
    : undefined;
}

/** The return URL from the OIDC state, only if it is a path inside the app (no open redirect). */
export function safeTargetUrl(state: string | undefined): string | undefined {
  let target = state ?? '';
  try {
    target = decodeURIComponent(target);
  } catch {
    return undefined;
  }
  return target.startsWith('/') && !target.startsWith('//') && !target.startsWith('/\\')
    ? target
    : undefined;
}
