import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  ConfirmService,
  EmptyState,
  FieldError,
  LoadingIndicator,
  NotificationService,
  PageHeader,
  SearchField,
} from '../../shared/ui';

interface SampleCustomer {
  id: number;
  name: string;
  email: string;
  city: string;
}

const cities = ['Berlin', 'Hamburg', 'München', 'Köln', 'Leipzig', 'Dresden', 'Bremen'];
const names = [
  'Anna Bauer', 'Ben Schulz', 'Clara Wolf', 'David Krüger', 'Emma Fischer', 'Felix Wagner', 'Greta Hoffmann',
  'Hannes Becker', 'Ida Richter', 'Jonas Koch', 'Klara Neumann', 'Lukas Schwarz', 'Mia Zimmermann',
  'Noah Braun', 'Olivia Hartmann', 'Paul Lange', 'Quirin Werner', 'Rosa Krause', 'Simon Meier', 'Tina Lehmann',
  'Udo Schmitt', 'Vera Schmid', 'Wilhelm Frank',
];

/**
 * Development-only page that shows every shared UI building block in a realistic list/form setting.
 * It is the reference for feature pages (customers, galleries, …). Data is local sample data.
 */
@Component({
  selector: 'app-ui-showcase',
  imports: [
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    PageHeader,
    SearchField,
    EmptyState,
    LoadingIndicator,
    FieldError,
  ],
  templateUrl: './ui-showcase.html',
  styleUrl: './ui-showcase.scss',
})
export class UiShowcase {
  private readonly confirmService = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  protected readonly columns = ['name', 'email', 'city', 'actions'];
  protected readonly customers = signal<SampleCustomer[]>(
    names.map((name, i) => ({
      id: i + 1,
      name,
      email: name.toLowerCase().replace(' ', '.').replace(/[äöü]/g, (c) => ({ ä: 'ae', ö: 'oe', ü: 'ue' })[c]!) + '@example.test',
      city: cities[i % cities.length],
    })),
  );
  protected readonly searchTerm = signal('');
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(5);
  protected readonly loading = signal(false);

  protected readonly filtered = computed(() => {
    const term = this.searchTerm().toLowerCase();
    return this.customers().filter(
      (c) => !term || c.name.toLowerCase().includes(term) || c.city.toLowerCase().includes(term),
    );
  });
  protected readonly page = computed(() =>
    this.filtered().slice(this.pageIndex() * this.pageSize(), (this.pageIndex() + 1) * this.pageSize()),
  );

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
  });

  protected onSearch(term: string): void {
    // Real lists load from the API here; the short delay shows the loading indicator
    this.loading.set(true);
    setTimeout(() => {
      this.searchTerm.set(term);
      this.pageIndex.set(0);
      this.loading.set(false);
    }, 400);
  }

  protected onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  protected async remove(customer: SampleCustomer): Promise<void> {
    const confirmed = await this.confirmService.confirm({
      title: 'Eintrag löschen?',
      message: `„${customer.name}“ wird endgültig gelöscht.`,
      confirmLabel: 'Löschen',
      destructive: true,
    });
    if (confirmed) {
      this.customers.update((list) => list.filter((c) => c.id !== customer.id));
      this.notifications.success(`„${customer.name}“ wurde gelöscht.`);
    }
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, email } = this.form.getRawValue();
    this.customers.update((list) => [{ id: Date.now(), name, email, city: '–' }, ...list]);
    this.notifications.success(`„${name}“ wurde gespeichert.`);
    this.form.reset();
  }

  protected showError(): void {
    // Same path as a failed API call with an RFC 9457 problem detail from the backend
    this.notifications.error(
      new HttpErrorResponse({
        status: 409,
        error: { status: 409, title: 'Conflict', detail: 'Beispiel: Diese E-Mail-Adresse ist bereits vergeben.' },
      }),
    );
  }
}
