import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Api } from '../api/api';
import { createTenant, getTenant, updateTenant } from '../api/functions';
import { CreateTenantRequest, TenantResponse, UpdateTenantRequest } from '../api/models';
import {
  FieldError,
  LoadingIndicator,
  NotificationService,
  PageHeader,
  apiErrorMessage,
} from '../shared/ui';
import { studioHints, studioPatterns, suggestSlug } from './studio-validators';

/**
 * Register a studio together with its first studio admin (/plattform/studios/neu) or change it
 * (/plattform/studios/:id): the name always, the first admin only until onboarding completes; the slug never.
 */
@Component({
  selector: 'app-studio-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatButtonModule,
    PageHeader,
    FieldError,
    LoadingIndicator,
  ],
  templateUrl: './studio-form.html',
  styleUrl: './studio-form.scss',
})
export class StudioForm implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notifications = inject(NotificationService);

  protected readonly hints = studioHints;
  protected readonly saving = signal(false);
  protected readonly loading = signal(false);
  /** The studio being edited; undefined while registering a new one. */
  protected readonly studio = signal<TenantResponse | undefined>(undefined);
  protected readonly editing = signal(false);
  protected readonly onboardingCompleted = computed(
    () => this.studio()?.onboardingStatus === 'COMPLETED',
  );
  protected readonly onboardingFailed = computed(() => {
    const studio = this.studio();
    return studio?.onboardingStatus === 'PENDING' && !!studio.onboardingError;
  });
  protected readonly title = computed(() => {
    if (!this.editing()) {
      return 'Studio registrieren';
    }
    return this.studio() ? `${this.studio()!.name} bearbeiten` : 'Studio bearbeiten';
  });
  protected readonly subtitle = computed(() => {
    if (!this.editing()) {
      return 'Keycloak-Organisation und erster Studio-Admin werden danach automatisch angelegt.';
    }
    return this.onboardingCompleted()
      ? 'Der Name lässt sich jederzeit ändern, das Kürzel nicht.'
      : 'Name und erster Studio-Admin lassen sich bis zum Abschluss des Onboardings ändern, das Kürzel nicht.';
  });

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    slug: ['', [Validators.required, Validators.pattern(studioPatterns.slug)]],
    adminEmail: [
      '',
      [Validators.required, Validators.maxLength(254), Validators.pattern(studioPatterns.email)],
    ],
    adminFirstName: ['', Validators.maxLength(100)],
    adminLastName: ['', Validators.maxLength(100)],
  });

  constructor() {
    // Suggest the slug from the name until the user edits the slug (new studios only)
    this.form.controls.name.valueChanges.pipe(takeUntilDestroyed()).subscribe((name) => {
      const slug = this.form.controls.slug;
      if (!this.editing() && !slug.dirty) {
        slug.setValue(suggestSlug(name));
      }
    });
  }

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      return;
    }
    this.editing.set(true);
    this.form.controls.slug.disable();
    this.loading.set(true);
    try {
      this.showStudio(await this.api.invoke(getTenant, { tenantId: id }));
    } catch (error) {
      this.notifications.error(error);
      void this.router.navigate(['/plattform/studios']);
    } finally {
      this.loading.set(false);
    }
  }

  protected async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const studio = this.studio();
    this.saving.set(true);
    try {
      if (studio) {
        await this.update(studio);
      } else {
        await this.register();
      }
      void this.router.navigate(['/plattform/studios']);
    } catch (error) {
      if (!studio && error instanceof HttpErrorResponse && error.status === 409) {
        // Slug already taken: show it at the field
        this.form.controls.slug.setErrors({ server: apiErrorMessage(error) });
        this.form.controls.slug.markAsTouched();
      } else {
        this.notifications.error(error);
      }
    } finally {
      this.saving.set(false);
    }
  }

  private async register(): Promise<void> {
    const value = this.form.getRawValue();
    const body: CreateTenantRequest = {
      name: value.name.trim(),
      slug: value.slug.trim(),
      ...this.adminData(),
    };
    await this.api.invoke(createTenant, { body });
    this.notifications.success(
      `„${body.name}“ wurde registriert. ${body.adminEmail} erhält eine Einladung, sobald das Onboarding abgeschlossen ist.`,
    );
  }

  private async update(studio: TenantResponse): Promise<void> {
    const body: UpdateTenantRequest = { name: this.form.getRawValue().name.trim() };
    if (!this.onboardingCompleted()) {
      Object.assign(body, this.adminData());
    }
    await this.api.invoke(updateTenant, { tenantId: studio.id, body });
    const retry = this.onboardingFailed()
      ? ' Das Onboarding wird mit den neuen Daten erneut versucht.'
      : '';
    this.notifications.success(`„${body.name}“ wurde gespeichert.${retry}`);
  }

  private adminData() {
    const value = this.form.getRawValue();
    const optional = (text: string) => (text.trim() ? text.trim() : undefined);
    return {
      adminEmail: value.adminEmail.trim(),
      adminFirstName: optional(value.adminFirstName),
      adminLastName: optional(value.adminLastName),
    };
  }

  private showStudio(studio: TenantResponse): void {
    this.studio.set(studio);
    this.form.reset({
      name: studio.name,
      slug: studio.slug,
      adminEmail: studio.adminEmail ?? '',
      adminFirstName: studio.adminFirstName ?? '',
      adminLastName: studio.adminLastName ?? '',
    });
    if (this.onboardingCompleted()) {
      // The admin manages the account in Keycloak; its data is no longer kept at the studio
      this.form.controls.adminEmail.disable();
      this.form.controls.adminFirstName.disable();
      this.form.controls.adminLastName.disable();
    }
  }
}
