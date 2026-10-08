import { isDevMode } from '@angular/core';
import { Routes } from '@angular/router';
import { platformGuard } from './auth/platform.guard';
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
      {
        path: 'preisliste',
        title: 'Preisliste',
        loadComponent: () => import('./price-list/price-list-page').then((m) => m.PriceListPage),
      },
      {
        path: 'preisliste/einstellungen',
        title: 'Steuersatz',
        loadComponent: () => import('./price-list/price-list-settings-form').then((m) => m.PriceListSettingsForm),
      },
      {
        path: 'preisliste/produkte/neu',
        title: 'Neues Produkt',
        loadComponent: () => import('./price-list/product-form').then((m) => m.ProductForm),
      },
      {
        path: 'preisliste/produkte/:id',
        title: 'Produkt bearbeiten',
        loadComponent: () => import('./price-list/product-form').then((m) => m.ProductForm),
      },
      {
        path: 'preisliste/pakete/neu',
        title: 'Neues Download-Paket',
        loadComponent: () => import('./price-list/download-package-form').then((m) => m.DownloadPackageForm),
      },
      {
        path: 'preisliste/pakete/:id',
        title: 'Download-Paket bearbeiten',
        loadComponent: () => import('./price-list/download-package-form').then((m) => m.DownloadPackageForm),
      },
      {
        path: 'preisliste/versandarten/neu',
        title: 'Neue Versandart',
        loadComponent: () => import('./price-list/shipping-method-form').then((m) => m.ShippingMethodForm),
      },
      {
        path: 'preisliste/versandarten/:id',
        title: 'Versandart bearbeiten',
        loadComponent: () => import('./price-list/shipping-method-form').then((m) => m.ShippingMethodForm),
      },
      {
        path: 'profil',
        title: 'Studio-Profil',
        loadComponent: () => import('./studio-profile/studio-profile-form').then((m) => m.StudioProfileForm),
      },
      {
        path: 'rechtstexte',
        title: 'Rechtstexte',
        loadComponent: () => import('./studio-profile/legal-texts-page').then((m) => m.LegalTextsPage),
      },
      {
        path: 'galerien',
        title: 'Galerien',
        loadComponent: () => import('./galleries/gallery-list').then((m) => m.GalleryList),
      },
      {
        path: 'galerien/neu',
        title: 'Neue Galerie',
        loadComponent: () => import('./galleries/gallery-form').then((m) => m.GalleryForm),
      },
      {
        path: 'galerien/:id',
        title: 'Galerie',
        loadComponent: () => import('./galleries/gallery-form').then((m) => m.GalleryForm),
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
  // --- platform area (#35) ---
  {
    path: 'plattform',
    canActivate: [platformGuard],
    loadComponent: () => import('./layout/platform-shell').then((m) => m.PlatformShell),
    // New platform pages: add a child route here and an entry in layout/platform-navigation.ts
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'studios' },
      {
        path: 'studios',
        title: 'Studios',
        loadComponent: () => import('./platform/studio-list').then((m) => m.StudioList),
      },
      {
        path: 'studios/neu',
        title: 'Studio registrieren',
        loadComponent: () => import('./platform/studio-form').then((m) => m.StudioForm),
      },
    ],
  },
  // --- end platform area ---
  { path: '**', redirectTo: '' },
];
