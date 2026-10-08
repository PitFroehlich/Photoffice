import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  MatAutocompleteModule,
  MatAutocompleteSelectedEvent,
} from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { ErrorStateMatcher, ShowOnDirtyErrorStateMatcher } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { Api } from '../api/api';
import {
  createGallery,
  deleteGallery,
  getGallery,
  listCustomers,
  publishGallery,
  unpublishGallery,
  updateGallery,
} from '../api/functions';
import { Customer, Gallery, GalleryCustomer, GalleryInput } from '../api/models';
import {
  ConfirmService,
  FieldError,
  LoadingIndicator,
  NotificationService,
  PageHeader,
  formatApiDate,
  fromApiDate,
  toApiDate,
  trimmedPattern,
} from '../shared/ui';
import { galleryStateClass, galleryStateLabel } from './gallery-labels';
import { galleryHints, galleryPatterns } from './gallery-validators';

/** Create (/studio/galerien/neu) or edit (/studio/galerien/:id) a gallery; publish/unpublish when editing. */
@Component({
  selector: 'app-gallery-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatAutocompleteModule,
    MatDatepickerModule,
    PageHeader,
    FieldError,
    LoadingIndicator,
  ],
  templateUrl: './gallery-form.html',
  styleUrl: './gallery-form.scss',
  // MatChipsModule provides its own (default) ErrorStateMatcher, which would hide errors while typing
  providers: [{ provide: ErrorStateMatcher, useClass: ShowOnDirtyErrorStateMatcher }],
})
export class GalleryForm implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly confirmService = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly hints = galleryHints;
  protected readonly gallery = signal<Gallery | undefined>(undefined);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly title = computed(() => this.gallery()?.name ?? 'Neue Galerie');
  protected readonly stateLabel = computed(() => {
    const gallery = this.gallery();
    return gallery ? galleryStateLabel(gallery) : '';
  });
  protected readonly stateClass = computed(() => {
    const gallery = this.gallery();
    return gallery ? galleryStateClass(gallery) : '';
  });
  protected readonly formatDate = formatApiDate;

  /** Earliest selectable expiry date: today. */
  protected readonly today = startOfToday();

  /** Assigned customers (chips). */
  protected readonly customers = signal<GalleryCustomer[]>([]);
  protected readonly customerSearch = new FormControl('', { nonNullable: true });
  protected readonly suggestions = signal<Customer[]>([]);

  protected readonly form = inject(FormBuilder).group({
    name: [
      '',
      [Validators.required, Validators.maxLength(100), trimmedPattern(galleryPatterns.name)],
    ],
    description: ['', Validators.maxLength(1000)],
    expiresOn: new FormControl<Date | null>(null),
  });

  constructor() {
    this.customerSearch.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((term) => void this.suggest(term));
  }

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      return;
    }
    this.loading.set(true);
    try {
      this.show(await this.api.invoke(getGallery, { galleryId: id }));
    } catch (error) {
      this.notifications.error(error);
      void this.router.navigate(['/studio/galerien']);
    } finally {
      this.loading.set(false);
    }
  }

  protected addCustomer(event: MatAutocompleteSelectedEvent): void {
    const customer = event.option.value as Customer;
    if (!this.customers().some((c) => c.id === customer.id)) {
      this.customers.update((list) => [
        ...list,
        {
          id: customer.id,
          firstName: customer.firstName,
          lastName: customer.lastName,
          email: customer.email,
        },
      ]);
      this.form.markAsDirty();
    }
    this.customerSearch.setValue('');
    this.suggestions.set([]);
  }

  protected removeCustomer(customer: GalleryCustomer): void {
    this.customers.update((list) => list.filter((c) => c.id !== customer.id));
    this.form.markAsDirty();
  }

  protected customerLabel(customer: Pick<Customer, 'firstName' | 'lastName'>): string {
    return `${customer.firstName} ${customer.lastName}`;
  }

  protected async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const body = this.toInput();
    const id = this.gallery()?.id;
    this.saving.set(true);
    try {
      if (id) {
        this.show(await this.api.invoke(updateGallery, { galleryId: id, body }));
        this.notifications.success(`„${body.name}“ wurde gespeichert.`);
      } else {
        const created = await this.api.invoke(createGallery, { body });
        this.notifications.success(`„${body.name}“ wurde angelegt.`);
        // Stay on the gallery: next steps are images (#9) and publishing
        void this.router.navigate(['/studio/galerien', created.id], { replaceUrl: true });
        this.show(created);
      }
    } catch (error) {
      this.notifications.error(error);
    } finally {
      this.saving.set(false);
    }
  }

  protected async publish(): Promise<void> {
    const gallery = this.gallery();
    if (!gallery) {
      return;
    }
    if (this.form.dirty) {
      this.notifications.error('Bitte speichern Sie zuerst Ihre Änderungen.');
      return;
    }
    try {
      this.show(await this.api.invoke(publishGallery, { galleryId: gallery.id }));
      this.notifications.success(`„${gallery.name}“ ist jetzt online.`);
    } catch (error) {
      this.notifications.error(error);
    }
  }

  protected async unpublish(): Promise<void> {
    const gallery = this.gallery();
    if (!gallery) {
      return;
    }
    const confirmed = await this.confirmService.confirm({
      title: 'Galerie offline nehmen?',
      message: `Kunden können „${gallery.name}“ danach nicht mehr aufrufen, bis Sie sie wieder veröffentlichen.`,
      confirmLabel: 'Offline nehmen',
    });
    if (!confirmed) {
      return;
    }
    try {
      this.show(await this.api.invoke(unpublishGallery, { galleryId: gallery.id }));
      this.notifications.success(`„${gallery.name}“ ist jetzt offline.`);
    } catch (error) {
      this.notifications.error(error);
    }
  }

  protected async remove(): Promise<void> {
    const gallery = this.gallery();
    if (!gallery) {
      return;
    }
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
      void this.router.navigate(['/studio/galerien']);
    } catch (error) {
      this.notifications.error(error);
    }
  }

  private async suggest(term: string): Promise<void> {
    if (typeof term !== 'string' || term.trim().length < 2) {
      this.suggestions.set([]);
      return;
    }
    try {
      const page = await this.api.invoke(listCustomers, { search: term.trim(), size: 10 });
      const assigned = new Set(this.customers().map((c) => c.id));
      this.suggestions.set(page.items.filter((c) => !assigned.has(c.id)));
    } catch (error) {
      this.notifications.error(error);
    }
  }

  private show(gallery: Gallery): void {
    this.gallery.set(gallery);
    this.customers.set(gallery.customers);
    this.form.reset({
      name: gallery.name,
      description: gallery.description ?? '',
      expiresOn: fromApiDate(gallery.expiresOn),
    });
  }

  private toInput(): GalleryInput {
    const value = this.form.getRawValue();
    const description = (value.description ?? '').trim();
    return {
      name: (value.name ?? '').trim(),
      ...(description ? { description } : {}),
      ...(value.expiresOn ? { expiresOn: toApiDate(value.expiresOn) } : {}),
      customerIds: this.customers().map((c) => c.id),
    };
  }
}

function startOfToday(): Date {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}
