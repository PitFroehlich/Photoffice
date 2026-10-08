import { Routes } from '@angular/router';
import { studioGuard } from './auth/studio.guard';
import { StartPage } from './start/start-page';

export const routes: Routes = [
  { path: '', component: StartPage },
  {
    path: 'studio',
    canActivate: [studioGuard],
    loadComponent: () => import('./studio/studio-home').then((m) => m.StudioHome),
  },
  { path: '**', redirectTo: '' },
];
