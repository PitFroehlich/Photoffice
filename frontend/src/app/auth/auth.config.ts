import { AuthConfig } from 'angular-oauth2-oidc';

/**
 * OpenID Connect settings for the studio login (Keycloak realm "photoffice").
 * Local development values; runtime configuration for other environments follows with the deployment setup.
 */
export const authConfig: AuthConfig = {
  issuer: 'http://localhost:8180/realms/photoffice',
  clientId: 'photoffice-frontend',
  responseType: 'code',
  redirectUri: `${window.location.origin}/studio`,
  postLogoutRedirectUri: window.location.origin,
  scope: 'openid profile email organization',
  // Plain HTTP is only accepted for localhost
  requireHttps: 'remoteOnly',
  showDebugInformation: false,
};

/** API calls that get the access token attached. */
export const apiUrls = ['/api/'];
