import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Api } from '../api/api';
import {
  listTenants,
  reactivateTenant,
  retryTenantOnboarding,
  suspendTenant,
} from '../api/functions';
import { TenantResponse } from '../api/models';
import {
  ConfirmService,
  EmptyState,
  LoadingIndicator,
  NotificationService,
  PageHeader,
} from '../shared/ui';

// Intl instead of DatePipe: DatePipe would pull Angular's common pipes into the initial bundle
const dateTimeFormat = new Intl.DateTimeFormat('de-DE', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

/** How often the list is reloaded while an onboarding is still running (it completes in the background). */
export const ONBOARDING_POLL_INTERVAL_MS = 3000;
/** Polling stops this long after the last load or action of the user (failed onboardings may stay pending). */
const ONBOARDING_POLL_DURATION_MS = 2 * 60 * 1000;

/**
 * All studios of the platform with status and onboarding status. The platform operator suspends/reactivates studios
 * and sees (and retries) failed onboardings here.
 */
@Component({
  selector: 'app-studio-list',
  imports: [
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    PageHeader,
    EmptyState,
    LoadingIndicator,
  ],
  templateUrl: './studio-list.html',
  styleUrl: './studio-list.scss',
})
export class StudioList implements OnInit {
  private readonly api = inject(Api);
  private readonly confirmService = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  protected readonly columns = ['name', 'slug', 'status', 'onboarding', 'createdAt', 'actions'];
  protected readonly studios = signal<TenantResponse[] | undefined>(undefined);
  protected readonly loading = signal(false);
  /** Studio ids with an action in progress (buttons disabled). */
  protected readonly busy = signal<ReadonlySet<string>>(new Set());

  protected readonly subtitle = computed(() => {
    const studios = this.studios();
    if (!studios) {
      return '';
    }
    const failed = studios.filter((studio) => this.onboardingFailed(studio)).length;
    const count = `${studios.length} ${studios.length === 1 ? 'Studio' : 'Studios'}`;
    return failed ? `${count}, davon ${failed} mit fehlgeschlagenem Onboarding` : count;
  });

  private pollTimer: ReturnType<typeof setTimeout> | undefined;
  private pollUntil = 0;
  private destroyed = false;

  constructor() {
    inject(DestroyRef).onDestroy(() => {
      this.destroyed = true;
      clearTimeout(this.pollTimer);
    });
  }

  ngOnInit(): void {
    void this.load();
  }

  protected refresh(): void {
    void this.load();
  }

  /** "08.10.2026, 14:33" (local time). */
  protected dateTime(iso: string | undefined): string {
    return iso ? dateTimeFormat.format(new Date(iso)) : '';
  }

  protected onboardingFailed(studio: TenantResponse): boolean {
    return studio.onboardingStatus === 'PENDING' && !!studio.onboardingError;
  }

  protected isBusy(studio: TenantResponse): boolean {
    return this.busy().has(studio.id);
  }

  protected async suspend(studio: TenantResponse): Promise<void> {
    const confirmed = await this.confirmService.confirm({
      title: 'Studio sperren?',
      message:
        `Die Benutzer von „${studio.name}“ können sich danach nicht mehr anmelden, ` +
        'bis das Studio wieder freigeschaltet wird. Die Daten des Studios bleiben erhalten.',
      confirmLabel: 'Sperren',
      destructive: true,
    });
    if (confirmed) {
      await this.run(
        studio,
        () => this.api.invoke(suspendTenant, { tenantId: studio.id }),
        `„${studio.name}“ ist gesperrt.`,
      );
    }
  }

  protected async reactivate(studio: TenantResponse): Promise<void> {
    const confirmed = await this.confirmService.confirm({
      title: 'Studio freischalten?',
      message: `Die Benutzer von „${studio.name}“ können sich danach wieder anmelden.`,
      confirmLabel: 'Freischalten',
    });
    if (confirmed) {
      await this.run(
        studio,
        () => this.api.invoke(reactivateTenant, { tenantId: studio.id }),
        `„${studio.name}“ ist wieder freigeschaltet.`,
      );
    }
  }

  protected async retryOnboarding(studio: TenantResponse): Promise<void> {
    await this.run(
      studio,
      () => this.api.invoke(retryTenantOnboarding, { tenantId: studio.id }),
      `Das Onboarding von „${studio.name}“ wird erneut versucht.`,
    );
  }

  private async run(
    studio: TenantResponse,
    action: () => Promise<TenantResponse>,
    success: string,
  ): Promise<void> {
    this.busy.update((ids) => new Set(ids).add(studio.id));
    try {
      const updated = await action();
      this.studios.update((studios) => studios?.map((s) => (s.id === updated.id ? updated : s)));
      this.notifications.success(success);
      this.pollUntil = Date.now() + ONBOARDING_POLL_DURATION_MS;
      await this.load(false);
    } catch (error) {
      this.notifications.error(error);
    } finally {
      this.busy.update((ids) => {
        const remaining = new Set(ids);
        remaining.delete(studio.id);
        return remaining;
      });
    }
  }

  private async load(showLoading = true): Promise<void> {
    clearTimeout(this.pollTimer);
    if (showLoading) {
      this.pollUntil = Date.now() + ONBOARDING_POLL_DURATION_MS;
    }
    this.loading.set(showLoading);
    try {
      this.studios.set(await this.api.invoke(listTenants));
    } catch (error) {
      this.notifications.error(error);
    } finally {
      this.loading.set(false);
    }
    this.schedulePoll();
  }

  /** Onboarding runs in the background: reload while a studio is still pending. */
  private schedulePoll(): void {
    const pending = this.studios()?.some((studio) => studio.onboardingStatus === 'PENDING');
    if (pending && !this.destroyed && Date.now() < this.pollUntil) {
      this.pollTimer = setTimeout(() => void this.load(false), ONBOARDING_POLL_INTERVAL_MS);
    }
  }
}
