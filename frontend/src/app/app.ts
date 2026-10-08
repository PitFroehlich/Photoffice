import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Api } from './api/api';
import { getSystemInfo } from './api/functions';
import { SystemInfo } from './api/models';

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App implements OnInit {
  private readonly api = inject(Api);

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
