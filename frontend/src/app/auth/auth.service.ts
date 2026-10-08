import { Injectable, inject, signal } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './auth.config';

/**
 * Wraps the OIDC client: login state, login and logout for studio users.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauthService = inject(OAuthService);

  private readonly loggedIn = signal(false);
  readonly isLoggedIn = this.loggedIn.asReadonly();

  /** Called once at application start; completes a running login (code in URL) if present. */
  async init(): Promise<void> {
    this.oauthService.configure(authConfig);
    this.oauthService.events.subscribe(() => this.loggedIn.set(this.oauthService.hasValidAccessToken()));
    try {
      await this.oauthService.loadDiscoveryDocumentAndTryLogin();
      this.oauthService.setupAutomaticSilentRefresh();
    } catch (error) {
      // Login server not reachable: the public part of the app keeps working
      console.error('OIDC initialisation failed', error);
    }
    this.loggedIn.set(this.oauthService.hasValidAccessToken());
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
  }
}
