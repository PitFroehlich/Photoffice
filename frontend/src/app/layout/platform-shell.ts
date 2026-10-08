import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { map } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { provideStudioUiDefaults } from '../shared/ui';
import { platformNavigation } from './platform-navigation';
import { SkipLink } from './skip-link';

/**
 * Frame of the platform area (platform operator): same layout as the studio back office – top bar with user menu,
 * side navigation and page content – but without studio. Access is checked by platformGuard.
 */
@Component({
  selector: 'app-platform-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
    SkipLink,
  ],
  template: `
    <app-skip-link />
    <mat-toolbar class="toolbar">
      @if (isSmallScreen()) {
        <button
          matIconButton
          type="button"
          aria-label="Navigation öffnen"
          (click)="sidenav.toggle()"
        >
          <mat-icon svgIcon="menu" />
        </button>
      }
      <a routerLink="/plattform" class="brand">
        <mat-icon svgIcon="photo_camera" aria-hidden="true" />
        <span>Photoffice</span>
      </a>
      <span class="area-name">Plattform-Verwaltung</span>
      <span class="spacer"></span>
      <button
        matButton
        type="button"
        class="user-button"
        [matMenuTriggerFor]="userMenu"
        aria-label="Benutzermenü"
      >
        <mat-icon svgIcon="account_circle" />
        <span class="user-name">{{ auth.displayName() }}</span>
      </button>
      <mat-menu #userMenu="matMenu" xPosition="before">
        <div class="menu-header">
          <div class="menu-name">{{ auth.displayName() }}</div>
          <div class="menu-detail">Plattform-Betreiber</div>
        </div>
        <mat-divider />
        <button mat-menu-item type="button" (click)="auth.logout()">
          <mat-icon svgIcon="logout" />
          <span>Abmelden</span>
        </button>
      </mat-menu>
    </mat-toolbar>

    <mat-sidenav-container class="container">
      <mat-sidenav
        #sidenav
        [mode]="isSmallScreen() ? 'over' : 'side'"
        [opened]="!isSmallScreen()"
        class="sidenav"
      >
        <nav aria-label="Plattform-Navigation">
          <mat-nav-list>
            @for (item of navigation; track item.link) {
              <a
                mat-list-item
                [routerLink]="item.link"
                routerLinkActive
                #active="routerLinkActive"
                [routerLinkActiveOptions]="{ exact: item.exact ?? false }"
                [activated]="active.isActive"
                (click)="isSmallScreen() && sidenav.close()"
              >
                <mat-icon matListItemIcon [svgIcon]="item.icon" />
                <span matListItemTitle>{{ item.label }}</span>
              </a>
            }
          </mat-nav-list>
        </nav>
      </mat-sidenav>

      <mat-sidenav-content>
        <main id="main-content" tabindex="-1" class="content">
          <router-outlet />
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  // Same frame as the studio area
  styleUrl: './studio-shell.scss',
  host: { class: 'density-compact' },
  // German paginator, outline form fields
  providers: [provideStudioUiDefaults()],
})
export class PlatformShell {
  protected readonly auth = inject(AuthService);
  protected readonly navigation = platformNavigation;

  protected readonly isSmallScreen = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 840px)')
      .pipe(map((state) => state.matches)),
    { initialValue: false },
  );
}
