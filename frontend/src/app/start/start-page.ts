import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Api } from '../api/api';
import { getSystemInfo } from '../api/functions';
import { SystemInfo } from '../api/models';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-start-page',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './start-page.html',
  styleUrl: './start-page.scss',
})
export class StartPage implements OnInit {
  private readonly api = inject(Api);
  protected readonly auth = inject(AuthService);

  protected readonly systemInfo = signal<SystemInfo | undefined>(undefined);
  protected readonly backendError = signal(false);

  async ngOnInit(): Promise<void> {
    try {
      this.systemInfo.set(await this.api.invoke(getSystemInfo));
    } catch {
      this.backendError.set(true);
    }
  }
}
