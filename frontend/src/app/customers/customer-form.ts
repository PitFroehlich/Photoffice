import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Api } from '../api/api';
import { createCustomer, deleteCustomer, getCustomer, updateCustomer } from '../api/functions';
import { Customer, CustomerInput } from '../api/models';
import { customerHints, customerPatterns } from './customer-validators';
import {
  ConfirmService,
  FieldError,
  LoadingIndicator,
  NotificationService,
  PageHeader,
  apiErrorMessage,
  trimmedPattern,
} from '../shared/ui';

/** Create (/studio/kunden/neu) or edit (/studio/kunden/:id) a customer. */
@Component({
  selector: 'app-customer-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    PageHeader,
    FieldError,
    LoadingIndicator,
  ],
  templateUrl: './customer-form.html',
  styleUrl: './customer-form.scss',
})
export class CustomerForm implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly confirmService = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  protected readonly hints = customerHints;
  protected readonly customerId = signal<string | undefined>(undefined);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly title = signal('Neuer Kunde');

  protected readonly form = inject(FormBuilder).nonNullable.group({
    firstName: [
      '',
      [Validators.required, Validators.maxLength(100), trimmedPattern(customerPatterns.name)],
    ],
    lastName: [
      '',
      [Validators.required, Validators.maxLength(100), trimmedPattern(customerPatterns.name)],
    ],
    email: [
      '',
      [Validators.required, Validators.maxLength(254), trimmedPattern(customerPatterns.email)],
    ],
    phone: ['', [Validators.maxLength(30), trimmedPattern(customerPatterns.phone)]],
    street: ['', [Validators.maxLength(200), trimmedPattern(customerPatterns.street)]],
    postalCode: ['', [Validators.maxLength(5), trimmedPattern(customerPatterns.postalCode)]],
    city: ['', [Validators.maxLength(100), trimmedPattern(customerPatterns.city)]],
    notes: ['', Validators.maxLength(2000)],
  });

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      return;
    }
    this.customerId.set(id);
    this.title.set('Kunde bearbeiten');
    this.loading.set(true);
    try {
      const customer = await this.api.invoke(getCustomer, { customerId: id });
      this.form.reset(this.toFormValue(customer));
      this.title.set(`${customer.firstName} ${customer.lastName}`);
    } catch (error) {
      this.notifications.error(error);
      void this.router.navigate(['/studio/kunden']);
    } finally {
      this.loading.set(false);
    }
  }

  protected async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const body = this.toInput();
    const name = `${body.firstName} ${body.lastName}`;
    const id = this.customerId();
    this.saving.set(true);
    try {
      if (id) {
        await this.api.invoke(updateCustomer, { customerId: id, body });
        this.notifications.success(`„${name}“ wurde gespeichert.`);
      } else {
        await this.api.invoke(createCustomer, { body });
        this.notifications.success(`„${name}“ wurde angelegt.`);
      }
      void this.router.navigate(['/studio/kunden']);
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 409) {
        // E-mail already used by another customer of the studio: show it at the field
        this.form.controls.email.setErrors({ server: apiErrorMessage(error) });
        this.form.controls.email.markAsTouched();
      } else {
        this.notifications.error(error);
      }
    } finally {
      this.saving.set(false);
    }
  }

  protected async remove(): Promise<void> {
    const id = this.customerId();
    if (!id) {
      return;
    }
    const confirmed = await this.confirmService.confirm({
      title: 'Kunden löschen?',
      message: `„${this.title()}“ wird endgültig gelöscht.`,
      confirmLabel: 'Löschen',
      destructive: true,
    });
    if (!confirmed) {
      return;
    }
    try {
      await this.api.invoke(deleteCustomer, { customerId: id });
      this.notifications.success(`„${this.title()}“ wurde gelöscht.`);
      void this.router.navigate(['/studio/kunden']);
    } catch (error) {
      this.notifications.error(error);
    }
  }

  private toInput(): CustomerInput {
    const value = this.form.getRawValue();
    const optional = (text: string) => (text.trim() ? text.trim() : undefined);
    return {
      firstName: value.firstName.trim(),
      lastName: value.lastName.trim(),
      email: value.email.trim(),
      phone: optional(value.phone),
      street: optional(value.street),
      postalCode: optional(value.postalCode),
      city: optional(value.city),
      notes: optional(value.notes),
    };
  }

  private toFormValue(customer: Customer) {
    return {
      firstName: customer.firstName,
      lastName: customer.lastName,
      email: customer.email,
      phone: customer.phone ?? '',
      street: customer.street ?? '',
      postalCode: customer.postalCode ?? '',
      city: customer.city ?? '',
      notes: customer.notes ?? '',
    };
  }
}
