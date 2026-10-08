import { isDevMode } from '@angular/core';
import { Routes } from '@angular/router';
import { studioGuard } from './auth/studio.guard';
import { PublicShell } from './layout/public-shell';
import { StartPage } from './start/start-page';

export const routes: Routes = [
  {
    path: '',
    component: PublicShell,
    children: [{ path: '', component: StartPage, title: 'Willkommen' }],
  },
  {
    path: 'studio',
    canActivate: [studioGuard],
    loadComponent: () => import('./layout/studio-shell').then((m) => m.StudioShell),
    // New studio pages: add a child route here and an entry in layout/studio-navigation.ts
    children: [
      {
        path: '',
        title: 'Übersicht',
        loadComponent: () => import('./studio/studio-home').then((m) => m.StudioHome),
      },
      {
        path: 'kunden',
        title: 'Kunden',
        loadComponent: () => import('./customers/customer-list').then((m) => m.CustomerList),
      },
      {
        path: 'kunden/neu',
        title: 'Neuer Kunde',
        loadComponent: () => import('./customers/customer-form').then((m) => m.CustomerForm),
      },
      {
        path: 'kunden/:id',
        title: 'Kunde bearbeiten',
        loadComponent: () => import('./customers/customer-form').then((m) => m.CustomerForm),
      },
      ...(isDevMode()
        ? [
            {
              path: 'ui-bausteine',
              title: 'UI-Bausteine',
              loadComponent: () => import('./studio/ui-showcase/ui-showcase').then((m) => m.UiShowcase),
            },
          ]
        : []),
    ],
  },
  { path: '**', redirectTo: '' },
];
