import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../api/api';
import { getSystemInfo } from '../api/functions';
import { SystemInfo } from '../api/models';

@Component({
  selector: 'app-start-page',
  imports: [RouterLink],
  templateUrl: './start-page.html',
})
export class StartPage implements OnInit {
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
