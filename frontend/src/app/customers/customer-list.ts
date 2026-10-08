import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Api } from '../api/api';
import { deleteCustomer, listCustomers } from '../api/functions';
import { Customer, CustomerPage } from '../api/models';
import {
  ConfirmService,
  EmptyState,
  LoadingIndicator,
  NotificationService,
  PageHeader,
  SearchField,
} from '../shared/ui';

/** Customers of the studio: server-side search and paging; state kept in the URL (?suche=&seite=&anzahl=). */
@Component({
  selector: 'app-customer-list',
  imports: [
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    PageHeader,
    SearchField,
    EmptyState,
    LoadingIndicator,
  ],
  templateUrl: './customer-list.html',
  styleUrl: './customer-list.scss',
})
export class CustomerList implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly confirmService = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  protected readonly columns = ['name', 'email', 'phone', 'city', 'actions'];
  protected readonly search = signal('');
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(25);
  protected readonly result = signal<CustomerPage | undefined>(undefined);
  protected readonly loading = signal(false);

  protected readonly subtitle = computed(() => {
    const total = this.result()?.totalElements;
    if (total === undefined) {
      return '';
    }
    if (this.search()) {
      return `${total} ${total === 1 ? 'Treffer' : 'Treffer'} für „${this.search()}“`;
    }
    return `${total} ${total === 1 ? 'Kunde' : 'Kunden'}`;
  });

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.search.set(params.get('suche') ?? '');
    this.pageIndex.set(Number(params.get('seite') ?? 0) || 0);
    this.pageSize.set(Number(params.get('anzahl') ?? 25) || 25);
    void this.load();
  }

  protected onSearch(term: string): void {
    this.search.set(term);
    this.pageIndex.set(0);
    void this.load();
  }

  protected onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    void this.load();
  }

  protected fullName(customer: Customer): string {
    return `${customer.lastName}, ${customer.firstName}`;
  }

  protected async remove(customer: Customer): Promise<void> {
    const name = `${customer.firstName} ${customer.lastName}`;
    const confirmed = await this.confirmService.confirm({
      title: 'Kunden löschen?',
      message: `„${name}“ wird endgültig gelöscht.`,
      confirmLabel: 'Löschen',
      destructive: true,
    });
    if (!confirmed) {
      return;
    }
    try {
      await this.api.invoke(deleteCustomer, { customerId: customer.id });
      this.notifications.success(`„${name}“ wurde gelöscht.`);
      // Last entry of a later page deleted: go back one page
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
        await this.api.invoke(listCustomers, {
          search: this.search() || undefined,
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

  private syncUrl(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      replaceUrl: true,
      queryParams: {
        suche: this.search() || null,
        seite: this.pageIndex() || null,
        anzahl: this.pageSize() === 25 ? null : this.pageSize(),
      },
    });
  }
}
