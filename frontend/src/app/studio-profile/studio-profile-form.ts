import { Component, DestroyRef, OnInit, Signal, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Api } from '../api/api';
import { formatDateTime } from './format-date-time';
import { getStudioProfile, updateStudioProfile } from '../api/functions';
import { Country, StudioProfile, StudioProfileInput } from '../api/models';
import { StudioSession } from '../studio/studio-session';
import {
  FieldError,
  LoadingIndicator,
  NotificationService,
  PageHeader,
  trimmedPattern,
} from '../shared/ui';
import {
  countryLabels,
  ibanValidator,
  postalCodeHints,
  postalCodeValidator,
  requiredWith,
  studioProfileHints,
  studioProfilePatterns,
} from './studio-profile-validators';

/** Everybody in the studio may read the profile and legal texts, only studio administrators may change them (#20). */
export function canEditStudioProfile(): Signal<boolean> {
  const session = inject(StudioSession);
  return computed(() => session.user()?.roles.includes('STUDIO_ADMIN') ?? false);
}

/** Master data of the studio (/studio/profil). */
@Component({
  selector: 'app-studio-profile-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    PageHeader,
    FieldError,
    LoadingIndicator,
  ],
  templateUrl: './studio-profile-form.html',
  styleUrl: './studio-profile-form.scss',
})
export class StudioProfileForm implements OnInit {
  private readonly api = inject(Api);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly hints = studioProfileHints;
  protected readonly countries = Object.entries(countryLabels) as [Country, string][];
  protected readonly canEdit = canEditStudioProfile();
  protected readonly formatDateTime = formatDateTime;
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly updatedAt = signal<string | undefined>(undefined);

  private readonly patterns = studioProfilePatterns;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    displayName: [
      '',
      [Validators.required, Validators.maxLength(100), trimmedPattern(this.patterns.name)],
    ],
    street: ['', [Validators.maxLength(200), trimmedPattern(this.patterns.street)]],
    postalCode: ['', [Validators.maxLength(5), postalCodeValidator('country')]],
    city: ['', [Validators.maxLength(100), trimmedPattern(this.patterns.city)]],
    country: ['DE' as Country, Validators.required],
    email: ['', [Validators.maxLength(254), trimmedPattern(this.patterns.email)]],
    phone: ['', [Validators.maxLength(30), trimmedPattern(this.patterns.phone)]],
    website: ['', [Validators.maxLength(200), trimmedPattern(this.patterns.website)]],
    taxNumber: ['', [Validators.maxLength(30), trimmedPattern(this.patterns.taxNumber)]],
    vatId: ['', [Validators.maxLength(30), trimmedPattern(this.patterns.vatId)]],
    accountHolder: [
      '',
      [Validators.maxLength(100), trimmedPattern(this.patterns.name), requiredWith('iban')],
    ],
    iban: ['', [Validators.maxLength(42), ibanValidator, requiredWith('accountHolder', 'bic')]],
    bic: ['', [Validators.maxLength(11), trimmedPattern(this.patterns.bic)]],
  });

  private readonly country = toSignal(this.form.controls.country.valueChanges, {
    initialValue: this.form.controls.country.value,
  });
  protected readonly postalCodeHint = computed(() => postalCodeHints[this.country()]);

  constructor() {
    const controls = this.form.controls;
    // Rules that depend on other fields are re-checked when those fields change
    controls.country.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => controls.postalCode.updateValueAndValidity());
    let revalidating = false;
    for (const changed of [controls.accountHolder, controls.iban, controls.bic]) {
      changed.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
        // The updates emit events themselves (FieldError follows statusChanges) – don't recurse
        if (revalidating) {
          return;
        }
        revalidating = true;
        try {
          controls.accountHolder.updateValueAndValidity();
          controls.iban.updateValueAndValidity();
        } finally {
          revalidating = false;
        }
      });
    }
  }

  async ngOnInit(): Promise<void> {
    try {
      const profile = await this.api.invoke(getStudioProfile);
      this.show(profile);
      if (!this.canEdit()) {
        this.form.disable();
      }
    } catch (error) {
      this.notifications.error(error);
    } finally {
      this.loading.set(false);
    }
  }

  protected async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    try {
      const profile = await this.api.invoke(updateStudioProfile, { body: this.toInput() });
      this.show(profile);
      this.notifications.success('Das Studio-Profil wurde gespeichert.');
    } catch (error) {
      this.notifications.error(error);
    } finally {
      this.saving.set(false);
    }
  }

  private show(profile: StudioProfile): void {
    this.form.reset({
      displayName: profile.displayName,
      street: profile.street ?? '',
      postalCode: profile.postalCode ?? '',
      city: profile.city ?? '',
      country: profile.country,
      email: profile.email ?? '',
      phone: profile.phone ?? '',
      website: profile.website ?? '',
      taxNumber: profile.taxNumber ?? '',
      vatId: profile.vatId ?? '',
      accountHolder: profile.accountHolder ?? '',
      iban: profile.iban ?? '',
      bic: profile.bic ?? '',
    });
    this.updatedAt.set(profile.updatedAt);
  }

  private toInput(): StudioProfileInput {
    const value = this.form.getRawValue();
    const optional = (text: string) => (text.trim() ? text.trim() : undefined);
    return {
      displayName: value.displayName.trim(),
      street: optional(value.street),
      postalCode: optional(value.postalCode),
      city: optional(value.city),
      country: value.country,
      email: optional(value.email),
      phone: optional(value.phone),
      website: optional(value.website),
      taxNumber: optional(value.taxNumber),
      vatId: optional(value.vatId),
      accountHolder: optional(value.accountHolder),
      iban: optional(value.iban),
      bic: optional(value.bic),
    };
  }
}
