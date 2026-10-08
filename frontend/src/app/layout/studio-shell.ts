import { Component, OnInit, computed, inject } from '@angular/core';
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
import { LoadingIndicator, provideStudioUiDefaults } from '../shared/ui';
import { StudioSession, roleLabels } from '../studio/studio-session';
import { SkipLink } from './skip-link';
import { studioNavigation } from './studio-navigation';

/**
 * Frame of the studio back office: top bar with studio and user menu, side navigation
 * (permanent on wide screens, overlay on small screens) and the page content.
 */
@Component({
  selector: 'app-studio-shell',
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
    LoadingIndicator,
    SkipLink,
  ],
  templateUrl: './studio-shell.html',
  styleUrl: './studio-shell.scss',
  host: { class: 'density-compact' },
  // German paginator, outline form fields for all studio pages
  providers: [provideStudioUiDefaults()],
})
export class StudioShell implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly session = inject(StudioSession);
  protected readonly navigation = studioNavigation;

  protected readonly isSmallScreen = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 840px)')
      .pipe(map((state) => state.matches)),
    { initialValue: false },
  );

  protected readonly roleLabel = computed(() => {
    const role = this.session.user()?.roles[0];
    return role ? (roleLabels[role] ?? role) : '';
  });

  ngOnInit(): void {
    void this.session.load();
  }

  protected logout(): void {
    this.session.clear();
    this.auth.logout();
  }
}
