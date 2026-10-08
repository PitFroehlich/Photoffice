import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { Api } from '../api/api';
import { createShippingMethod, getShippingMethod, updateShippingMethod } from '../api/functions';
import { ShippingMethodInput } from '../api/models';
import {
  FieldError,
  LoadingIndicator,
  NotificationService,
  PageHeader,
  apiErrorMessage,
} from '../shared/ui';
import { centsToInput, parsePriceCents, priceValidator } from './money';
import { canEditPriceList } from './price-list-labels';

/** Create (/studio/preisliste/versandarten/neu) or edit (/studio/preisliste/versandarten/:id) a shipping method. */
@Component({
  selector: 'app-shipping-method-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatButtonModule,
    PageHeader,
    FieldError,
    LoadingIndicator,
  ],
  templateUrl: './shipping-method-form.html',
  styleUrl: './price-list-form.scss',
})
export class ShippingMethodForm implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notifications = inject(NotificationService);

  protected readonly canEdit = canEditPriceList();
  protected readonly shippingMethodId = signal<string | undefined>(undefined);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly title = signal('Neue Versandart');

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    price: ['', [Validators.required, priceValidator]],
    active: [true],
  });

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      return;
    }
    this.shippingMethodId.set(id);
    this.title.set('Versandart bearbeiten');
    this.loading.set(true);
    try {
      const shippingMethod = await this.api.invoke(getShippingMethod, { shippingMethodId: id });
      this.form.reset({
        name: shippingMethod.name,
        price: centsToInput(shippingMethod.priceCents),
        active: shippingMethod.active !== false,
      });
      this.title.set(shippingMethod.name);
    } catch (error) {
      this.notifications.error(error);
      void this.router.navigate(['/studio/preisliste']);
    } finally {
      this.loading.set(false);
    }
  }

  protected async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const body: ShippingMethodInput = {
      name: value.name.trim(),
      priceCents: parsePriceCents(value.price)!,
      active: value.active,
    };
    const id = this.shippingMethodId();
    this.saving.set(true);
    try {
      if (id) {
        await this.api.invoke(updateShippingMethod, { shippingMethodId: id, body });
        this.notifications.success(`„${body.name}“ wurde gespeichert.`);
      } else {
        await this.api.invoke(createShippingMethod, { body });
        this.notifications.success(`„${body.name}“ wurde angelegt.`);
      }
      void this.router.navigate(['/studio/preisliste']);
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 409) {
        this.form.controls.name.setErrors({ server: apiErrorMessage(error) });
        this.form.controls.name.markAsTouched();
      } else {
        this.notifications.error(error);
      }
    } finally {
      this.saving.set(false);
    }
  }
}
