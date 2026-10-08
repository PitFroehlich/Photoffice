import { Component, computed, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { PageHeader } from '../shared/ui';
import { StudioSession, roleLabels } from './studio-session';

@Component({
  selector: 'app-studio-home',
  imports: [PageHeader, MatCardModule, MatChipsModule, MatIconModule],
  templateUrl: './studio-home.html',
  styleUrl: './studio-home.scss',
})
export class StudioHome {
  protected readonly session = inject(StudioSession);

  protected readonly roles = computed(() => (this.session.user()?.roles ?? []).map((r) => roleLabels[r] ?? r));
}
