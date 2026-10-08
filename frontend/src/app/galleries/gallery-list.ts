import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Api } from '../api/api';
import { deleteGallery, listGalleries } from '../api/functions';
import { Gallery, GalleryPage, GalleryStatus } from '../api/models';
import {
  ConfirmService,
  EmptyState,
  LoadingIndicator,
  NotificationService,
  PageHeader,
  SearchField,
  formatApiDate,
} from '../shared/ui';
import { customerNames, galleryStateClass, galleryStateLabel } from './gallery-labels';

type StatusFilter = GalleryStatus | 'ALL';

/** Galleries of the studio: search, status filter, optional customer filter (?kunde=), state in the URL. */
@Component({
  selector: 'app-gallery-list',
  imports: [
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatTooltipModule,
    PageHeader,
    SearchField,
    EmptyState,
    LoadingIndicator,
  ],
  templateUrl: './gallery-list.html',
  styleUrl: './gallery-list.scss',
})
export class GalleryList implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly confirmService = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  protected readonly columns = ['name', 'customers', 'status', 'expiresOn', 'actions'];
  protected readonly search = signal('');
  protected readonly status = signal<StatusFilter>('ALL');
  protected readonly customerId = signal<string | undefined>(undefined);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(25);
  protected readonly result = signal<GalleryPage | undefined>(undefined);
  protected readonly loading = signal(false);

  protected readonly filtered = computed(
    () => !!this.search() || this.status() !== 'ALL' || !!this.customerId(),
  );
  protected readonly subtitle = computed(() => {
    const total = this.result()?.totalElements;
    if (total === undefined) {
      return '';
    }
    return `${total} ${total === 1 ? 'Galerie' : 'Galerien'}${this.filtered() ? ' (gefiltert)' : ''}`;
  });

  protected readonly stateLabel = galleryStateLabel;
  protected readonly stateClass = galleryStateClass;
  protected readonly customerNames = customerNames;
  protected readonly formatDate = formatApiDate;

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.search.set(params.get('suche') ?? '');
    this.status.set((params.get('status')?.toUpperCase() as StatusFilter) ?? 'ALL');
    this.customerId.set(params.get('kunde') ?? undefined);
    this.pageIndex.set(Number(params.get('seite') ?? 0) || 0);
    void this.load();
  }

  protected onSearch(term: string): void {
    this.search.set(term);
    this.pageIndex.set(0);
    void this.load();
  }

  protected onStatus(status: StatusFilter): void {
    this.status.set(status);
    this.pageIndex.set(0);
    void this.load();
  }

  protected clearCustomerFilter(): void {
    this.customerId.set(undefined);
    this.pageIndex.set(0);
    void this.load();
  }

  protected onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    void this.load();
  }

  protected async remove(gallery: Gallery): Promise<void> {
    const confirmed = await this.confirmService.confirm({
      title: 'Galerie löschen?',
      message: `„${gallery.name}“ wird mit allen Bildern endgültig gelöscht. Kunden haben danach keinen Zugriff mehr.`,
      confirmLabel: 'Löschen',
      destructive: true,
    });
    if (!confirmed) {
      return;
    }
    try {
      await this.api.invoke(deleteGallery, { galleryId: gallery.id });
      this.notifications.success(`„${gallery.name}“ wurde gelöscht.`);
      if (this.result()?.items.length === 1 && this.pageIndex() > 0) {
        this.pageIndex.update((page) => page - 1);
      }
      await this.load();
    } catch (error) {
      this.notifications.error(error);
    }
  }

  private async load(): Promise<void> {
    this.syncUrl();
    this.loading.set(true);
    try {
      this.result.set(
        await this.api.invoke(listGalleries, {
          search: this.search() || undefined,
          status: this.status() === 'ALL' ? undefined : (this.status() as GalleryStatus),
          customerId: this.customerId(),
          page: this.pageIndex(),
          size: this.pageSize(),
        }),
      );
    } catch (error) {
      this.notifications.error(error);
    } finally {
      this.loading.set(false);
    }
  }

  /** Keeps the list state in the URL – only navigates if something changed (no stray navigation on init). */
  private syncUrl(): void {
    const queryParams = this.urlState();
    const current = this.route.snapshot.queryParamMap;
    const unchanged = Object.entries(queryParams).every(
      ([key, value]) => (current.get(key) ?? null) === value,
    );
    if (unchanged) {
      return;
    }
    void this.router.navigate([], {
      relativeTo: this.route,
      replaceUrl: true,
      queryParams,
    });
  }

  private urlState(): Record<string, string | null> {
    return {
      suche: this.search() || null,
      status: this.status() === 'ALL' ? null : this.status().toLowerCase(),
      kunde: this.customerId() ?? null,
      seite: this.pageIndex() ? String(this.pageIndex()) : null,
    };
  }
}
