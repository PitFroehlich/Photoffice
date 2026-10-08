import { Injectable, inject, signal } from '@angular/core';
import { Api } from '../api/api';
import { getCurrentStudioUser } from '../api/functions';
import { CurrentStudioUser } from '../api/models';

export const roleLabels: Record<string, string> = {
  STUDIO_ADMIN: 'Studio-Administrator',
  PHOTOGRAPHER: 'Fotograf',
};

/**
 * The logged-in studio user and their studio (from GET /api/studio/me), loaded once by the studio shell.
 * Pages in the studio area read it instead of calling the endpoint again.
 */
@Injectable({ providedIn: 'root' })
export class StudioSession {
  private readonly api = inject(Api);

  private readonly currentUser = signal<CurrentStudioUser | undefined>(undefined);
  private readonly state = signal<'idle' | 'loading' | 'ready' | 'denied'>('idle');

  readonly user = this.currentUser.asReadonly();
  readonly status = this.state.asReadonly();

  async load(): Promise<void> {
    if (this.state() === 'loading' || this.state() === 'ready') {
      return;
    }
    this.state.set('loading');
    try {
      this.currentUser.set(await this.api.invoke(getCurrentStudioUser));
      this.state.set('ready');
    } catch {
      this.currentUser.set(undefined);
      this.state.set('denied');
    }
  }

  clear(): void {
    this.currentUser.set(undefined);
    this.state.set('idle');
  }
}
