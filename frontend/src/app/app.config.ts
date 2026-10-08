import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { TitleStrategy, provideRouter } from '@angular/router';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { AppTitleStrategy } from './app-title-strategy';
import { routes } from './app.routes';
import { apiUrls } from './auth/auth.config';
import { AuthService } from './auth/auth.service';
import { provideUiDefaults } from './shared/ui/ui-providers';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // The OIDC library attaches the access token via a DI-based interceptor
    provideHttpClient(withInterceptorsFromDi()),
    provideOAuthClient({ resourceServer: { allowedUrls: apiUrls, sendAccessToken: true } }),
    provideAppInitializer(() => inject(AuthService).init()),
    provideRouter(routes),
    { provide: TitleStrategy, useClass: AppTitleStrategy },
    provideUiDefaults(),
  ],
};
