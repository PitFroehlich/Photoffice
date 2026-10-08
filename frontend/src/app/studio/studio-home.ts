import { Component, inject, OnInit, signal } from '@angular/core';
import { Api } from '../api/api';
import { getCurrentStudioUser } from '../api/functions';
import { CurrentStudioUser } from '../api/models';

const roleLabels: Record<string, string> = {
  STUDIO_ADMIN: 'Studio-Administrator',
  PHOTOGRAPHER: 'Fotograf',
};

@Component({
  selector: 'app-studio-home',
  templateUrl: './studio-home.html',
})
export class StudioHome implements OnInit {
  private readonly api = inject(Api);

  protected readonly user = signal<CurrentStudioUser | undefined>(undefined);
  protected readonly accessDenied = signal(false);

  async ngOnInit(): Promise<void> {
    try {
      this.user.set(await this.api.invoke(getCurrentStudioUser));
    } catch {
      this.accessDenied.set(true);
    }
  }

  protected roleLabel(role: string): string {
    return roleLabels[role] ?? role;
  }
}
