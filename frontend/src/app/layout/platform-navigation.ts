import { NavigationItem } from './studio-navigation';

/**
 * Entries of the platform side navigation. New platform pages add their entry here (and a child route in
 * app.routes.ts under "plattform").
 */
export const platformNavigation: NavigationItem[] = [
  { label: 'Studios', icon: 'storefront', link: '/plattform/studios' },
];
