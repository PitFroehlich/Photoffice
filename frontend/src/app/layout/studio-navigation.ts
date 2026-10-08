import { isDevMode } from '@angular/core';

export interface NavigationItem {
  label: string;
  /** Material Symbols name (outlined), see provideUiDefaults */
  icon: string;
  link: string;
  /** Highlight only on exactly this URL (default: also on sub pages). */
  exact?: boolean;
}

/**
 * Entries of the studio side navigation. New studio pages add their entry here (and a child route in
 * app.routes.ts under "studio").
 */
export const studioNavigation: NavigationItem[] = [
  { label: 'Übersicht', icon: 'dashboard', link: '/studio', exact: true },
  { label: 'Kunden', icon: 'group', link: '/studio/kunden' },
  { label: 'Preisliste', icon: 'sell', link: '/studio/preisliste' },
  // Showcase of the shared UI building blocks – development builds only
  ...(isDevMode() ? [{ label: 'UI-Bausteine', icon: 'widgets', link: '/studio/ui-bausteine' }] : []),
];
