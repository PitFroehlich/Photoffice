import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Api } from '../api/api';
import {
  listTenants,
  reactivateTenant,
  retryTenantOnboarding,
  suspendTenant,
} from '../api/functions';
import { TenantPage, TenantResponse } from '../api/models';
import {
  ConfirmService,
  EmptyState,
  LoadingIndicator,
  NotificationService,
  PageHeader,
  SearchField,
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

const DEFAULT_PAGE_SIZE = 25;

/** Filter values: German in the URL, API values in the request. */
export const statusFilters = [
  { url: 'aktiv', api: 'ACTIVE', label: 'Aktiv' },
  { url: 'gesperrt', api: 'SUSPENDED', label: 'Gesperrt' },
] as const;

export const onboardingFilters = [
  { url: 'offen', api: 'PENDING', label: 'Nicht abgeschlossen' },
  { url: 'fehlgeschlagen', api: 'FAILED', label: 'Fehlgeschlagen' },
  { url: 'abgeschlossen', api: 'COMPLETED', label: 'Abgeschlossen' },
] as const;

type StatusFilter = (typeof statusFilters)[number]['url'];
type OnboardingFilter = (typeof onboardingFilters)[number]['url'];

/**
 * Studios of the platform with status and onboarding status: server-side search (name, slug, admin e-mail),
 * filters and paging, state kept in the URL (?suche=&status=&onboarding=&seite=&anzahl=). The platform operator
 * suspends/reactivates studios and sees (and retries or corrects) failed onboardings here.
 */
@Component({
  selector: 'app-studio-list',
  imports: [
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    PageHeader,
    SearchField,
    EmptyState,
    LoadingIndicator,
  ],
  templateUrl: './studio-list.html',
  styleUrl: './studio-list.scss',
})
export class StudioList implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly confirmService = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  protected readonly statusFilters = statusFilters;
  protected readonly onboardingFilters = onboardingFilters;
  protected readonly columns = ['name', 'slug', 'status', 'onboarding', 'createdAt', 'actions'];
  protected readonly search = signal('');
  protected readonly status = signal<StatusFilter | ''>('');
  protected readonly onboarding = signal<OnboardingFilter | ''>('');
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(DEFAULT_PAGE_SIZE);
  protected readonly result = signal<TenantPage | undefined>(undefined);
  protected readonly loading = signal(false);
  /** Studio ids with an action in progress (buttons disabled). */
  protected readonly busy = signal<ReadonlySet<string>>(new Set());

  /** Query parameters of the current view; also handed to the form so that it returns to this view. */
  protected readonly listQuery = computed(() => ({
    suche: this.search() || null,
    status: this.status() || null,
    onboarding: this.onboarding() || null,
    seite: this.pageIndex() || null,
    anzahl: this.pageSize() === DEFAULT_PAGE_SIZE ? null : this.pageSize(),
  }));

  protected readonly filtered = computed(
    () => !!(this.search() || this.status() || this.onboarding()),
  );

  protected readonly subtitle = computed(() => {
    const result = this.result();
    if (!result) {
      return '';
    }
    const total = result.totalElements;
    const failed = result.failedOnboardings;
    if (!this.filtered()) {
      const count = `${total} ${total === 1 ? 'Studio' : 'Studios'}`;
      return failed ? `${count}, davon ${failed} mit fehlgeschlagenem Onboarding` : count;
    }
    const hits = this.search()
      ? `${total} Treffer für „${this.search()}“`
      : `${total} ${total === 1 ? 'Studio' : 'Studios'} gefunden`;
    return failed ? `${hits} (insgesamt ${failed} mit fehlgeschlagenem Onboarding)` : hits;
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
    const params = this.route.snapshot.queryParamMap;
    this.search.set(params.get('suche') ?? '');
    this.status.set(urlValue(statusFilters, params.get('status')));
    this.onboarding.set(urlValue(onboardingFilters, params.get('onboarding')));
    this.pageIndex.set(Math.max(0, Number(params.get('seite') ?? 0) || 0));
    this.pageSize.set(Number(params.get('anzahl') ?? DEFAULT_PAGE_SIZE) || DEFAULT_PAGE_SIZE);
    void this.load();
  }

  protected refresh(): void {
    void this.load();
  }

  protected onSearch(term: string): void {
    this.search.set(term);
    this.pageIndex.set(0);
    void this.load();
  }

  protected onStatus(value: StatusFilter | ''): void {
    this.status.set(value);
    this.pageIndex.set(0);
    void this.load();
  }

  protected onOnboarding(value: OnboardingFilter | ''): void {
    this.onboarding.set(value);
    this.pageIndex.set(0);
    void this.load();
  }

  /** "Nur fehlgeschlagene anzeigen" in the header. */
  protected showFailed(): void {
    this.onOnboarding('fehlgeschlagen');
  }

  protected onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
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
      this.result.update(
        (page) =>
          page && {
            ...page,
            items: page.items.map((s) => (s.id === updated.id ? updated : s)),
          },
      );
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
      this.syncUrl();
      this.pollUntil = Date.now() + ONBOARDING_POLL_DURATION_MS;
    }
    this.loading.set(showLoading);
    try {
      const page = await this.api.invoke(listTenants, {
        search: this.search() || undefined,
        status: apiValue(statusFilters, this.status()),
        onboarding: apiValue(onboardingFilters, this.onboarding()),
        page: this.pageIndex(),
        size: this.pageSize(),
      });
      if (page.items.length === 0 && page.totalElements > 0 && this.pageIndex() > 0) {
        // The page no longer exists (e.g. studios changed or an old link): show the last page
        this.pageIndex.set(Math.max(0, Math.ceil(page.totalElements / this.pageSize()) - 1));
        return this.load(showLoading);
      }
      this.result.set(page);
    } catch (error) {
      this.notifications.error(error);
    } finally {
      this.loading.set(false);
    }
    this.schedulePoll();
  }

  /** Onboarding runs in the background: reload while a studio on this page is still pending. */
  private schedulePoll(): void {
    const pending = this.result()?.items.some((studio) => studio.onboardingStatus === 'PENDING');
    if (pending && !this.destroyed && Date.now() < this.pollUntil) {
      this.pollTimer = setTimeout(() => void this.load(false), ONBOARDING_POLL_INTERVAL_MS);
    }
  }

  private syncUrl(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      replaceUrl: true,
      queryParams: this.listQuery(),
    });
  }
}

function urlValue<T extends { url: string }>(
  filters: readonly T[],
  value: string | null,
): T['url'] | '' {
  return filters.find((filter) => filter.url === value)?.url ?? '';
}

function apiValue<T extends { url: string; api: string }>(
  filters: readonly T[],
  url: string,
): T['api'] | undefined {
  return filters.find((filter) => filter.url === url)?.api;
}
