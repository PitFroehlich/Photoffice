import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Api } from '../api/api';
import { getPriceList, updatePriceListSettings } from '../api/functions';
import { FieldError, LoadingIndicator, NotificationService, PageHeader } from '../shared/ui';
import { canEditPriceList } from './price-list-labels';

/** "19", "7", "7,5" or "7.25" – at most two decimal places. */
const VAT_RATE_PATTERN = /^\d{1,2}(?:[.,]\d{1,2})?$/;

/** VAT rate of the price list (/studio/preisliste/einstellungen). The currency is fixed to EUR for now. */
@Component({
  selector: 'app-price-list-settings-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    PageHeader,
    FieldError,
    LoadingIndicator,
  ],
  templateUrl: './price-list-settings-form.html',
  styleUrl: './price-list-form.scss',
})
export class PriceListSettingsForm implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  protected readonly canEdit = canEditPriceList();
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    vatRate: ['', [Validators.required, Validators.pattern(VAT_RATE_PATTERN)]],
  });

  async ngOnInit(): Promise<void> {
    try {
      const priceList = await this.api.invoke(getPriceList);
      this.form.reset({ vatRate: String(priceList.settings.vatRatePercent).replace('.', ',') });
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
    this.saving.set(true);
    try {
      const vatRatePercent = Number(this.form.getRawValue().vatRate.replace(',', '.'));
      await this.api.invoke(updatePriceListSettings, { body: { vatRatePercent } });
      this.notifications.success('Der Steuersatz wurde gespeichert.');
      void this.router.navigate(['/studio/preisliste']);
    } catch (error) {
      this.notifications.error(error);
    } finally {
      this.saving.set(false);
    }
  }
}
