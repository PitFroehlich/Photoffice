import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Api } from '../api/api';
import { createTenant } from '../api/functions';
import { CreateTenantRequest } from '../api/models';
import { FieldError, NotificationService, PageHeader, apiErrorMessage } from '../shared/ui';
import { studioHints, studioPatterns, suggestSlug } from './studio-validators';

/** Register a studio (/plattform/studios/neu) together with its first studio admin. */
@Component({
  selector: 'app-studio-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    PageHeader,
    FieldError,
  ],
  templateUrl: './studio-form.html',
  styleUrl: './studio-form.scss',
})
export class StudioForm {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  protected readonly hints = studioHints;
  protected readonly saving = signal(false);

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
    // Suggest the slug from the name until the user edits the slug
    this.form.controls.name.valueChanges.pipe(takeUntilDestroyed()).subscribe((name) => {
      const slug = this.form.controls.slug;
      if (!slug.dirty) {
        slug.setValue(suggestSlug(name));
      }
    });
  }

  protected async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const body = this.toRequest();
    this.saving.set(true);
    try {
      await this.api.invoke(createTenant, { body });
      this.notifications.success(
        `„${body.name}“ wurde registriert. ${body.adminEmail} erhält eine Einladung, sobald das Onboarding abgeschlossen ist.`,
      );
      void this.router.navigate(['/plattform/studios']);
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 409) {
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

  private toRequest(): CreateTenantRequest {
    const value = this.form.getRawValue();
    const optional = (text: string) => (text.trim() ? text.trim() : undefined);
    return {
      name: value.name.trim(),
      slug: value.slug.trim(),
      adminEmail: value.adminEmail.trim(),
      adminFirstName: optional(value.adminFirstName),
      adminLastName: optional(value.adminLastName),
    };
  }
}
