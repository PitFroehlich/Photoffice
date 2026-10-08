import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthService } from '../auth/auth.service';
import { SkipLink } from './skip-link';

/** Frame for public pages (start page, later the customer gallery). */
@Component({
  selector: 'app-public-shell',
  imports: [RouterOutlet, RouterLink, MatToolbarModule, MatButtonModule, MatIconModule, SkipLink],
  template: `
    <app-skip-link />
    <mat-toolbar class="toolbar">
      <a routerLink="/" class="brand">
        <mat-icon svgIcon="photo_camera" aria-hidden="true" />
        <span>Photoffice</span>
      </a>
      <span class="spacer"></span>
      @if (auth.isLoggedIn()) {
        <a matButton="tonal" routerLink="/studio">Zum Studio-Bereich</a>
      } @else {
        <button matButton="filled" type="button" (click)="auth.login()">Studio-Login</button>
      }
    </mat-toolbar>
    <main id="main-content" tabindex="-1">
      <router-outlet />
    </main>
  `,
  styles: `
    .toolbar {
      background: var(--mat-sys-surface-container-low);
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .brand {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      color: var(--mat-sys-primary);
      text-decoration: none;
      font: var(--mat-sys-title-large);
    }
    .spacer {
      flex: 1;
    }
    main {
      outline: none;
    }
  `,
})
export class PublicShell {
  protected readonly auth = inject(AuthService);
}
