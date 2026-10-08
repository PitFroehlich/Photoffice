import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { trimmedPattern } from '../shared/ui/validators';
import { priceListHints, priceListPatterns } from './price-list-validators';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { Api } from '../api/api';
import { createProduct, getProduct, updateProduct } from '../api/functions';
import { DownloadResolution, Product, ProductInput, ProductType } from '../api/models';
import {
  FieldError,
  LoadingIndicator,
  NotificationService,
  PageHeader,
  apiErrorMessage,
} from '../shared/ui';
import { centsToInput, parsePriceCents, priceValidator } from './money';
import { canEditPriceList, productLabel, resolutionLabels } from './price-list-labels';

/**
 * Create (/studio/preisliste/produkte/neu?typ=abzug|download) or edit (/studio/preisliste/produkte/:id)
 * a product with its single price. The product type cannot be changed after creation.
 */
@Component({
  selector: 'app-product-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatButtonModule,
    PageHeader,
    FieldError,
    LoadingIndicator,
  ],
  templateUrl: './product-form.html',
  styleUrl: './price-list-form.scss',
})
export class ProductForm implements OnInit {
  protected readonly hints = priceListHints;
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notifications = inject(NotificationService);

  protected readonly canEdit = canEditPriceList();
  protected readonly productId = signal<string | undefined>(undefined);
  protected readonly type = signal<ProductType>('PRINT');
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly title = signal('Neuer Abzug');
  protected readonly resolutions = Object.entries(resolutionLabels) as [
    DownloadResolution,
    string,
  ][];

  protected readonly form = inject(FormBuilder).nonNullable.group({
    paperType: ['', [Validators.required, Validators.maxLength(50), trimmedPattern(priceListPatterns.paperType)]],
    printFormat: ['', [Validators.required, Validators.maxLength(50), trimmedPattern(priceListPatterns.printFormat)]],
    resolution: ['' as DownloadResolution | '', Validators.required],
    price: ['', [Validators.required, priceValidator]],
    active: [true],
  });

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.useType(
        this.route.snapshot.queryParamMap.get('typ') === 'download' ? 'DOWNLOAD' : 'PRINT',
      );
      this.title.set(this.type() === 'PRINT' ? 'Neuer Abzug' : 'Neuer Download');
      return;
    }
    this.productId.set(id);
    this.title.set('Produkt bearbeiten');
    this.loading.set(true);
    try {
      const product = await this.api.invoke(getProduct, { productId: id });
      this.useType(product.type);
      this.form.reset(this.toFormValue(product));
      this.title.set(productLabel(product));
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
    const body = this.toInput();
    const name = productLabel(body);
    const id = this.productId();
    this.saving.set(true);
    try {
      if (id) {
        await this.api.invoke(updateProduct, { productId: id, body });
        this.notifications.success(`„${name}“ wurde gespeichert.`);
      } else {
        await this.api.invoke(createProduct, { body });
        this.notifications.success(`„${name}“ wurde angelegt.`);
      }
      void this.router.navigate(['/studio/preisliste']);
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 409) {
        // Same paper × format (or resolution) already in the price list: show it at the field
        const control =
          this.type() === 'PRINT' ? this.form.controls.printFormat : this.form.controls.resolution;
        control.setErrors({ server: apiErrorMessage(error) });
        control.markAsTouched();
      } else {
        this.notifications.error(error);
      }
    } finally {
      this.saving.set(false);
    }
  }

  /** Only the fields of the type take part in validation; the others are disabled. */
  private useType(type: ProductType): void {
    this.type.set(type);
    const { paperType, printFormat, resolution } = this.form.controls;
    if (type === 'PRINT') {
      resolution.disable();
    } else {
      paperType.disable();
      printFormat.disable();
    }
  }

  private toInput(): ProductInput {
    const value = this.form.getRawValue();
    const common = { priceCents: parsePriceCents(value.price)!, active: value.active };
    if (this.type() === 'PRINT') {
      return {
        type: 'PRINT',
        paperType: value.paperType.trim(),
        printFormat: value.printFormat.trim(),
        ...common,
      };
    }
    return { type: 'DOWNLOAD', resolution: value.resolution as DownloadResolution, ...common };
  }

  private toFormValue(product: Product) {
    return {
      paperType: product.paperType ?? '',
      printFormat: product.printFormat ?? '',
      resolution: product.resolution ?? ('' as const),
      price: centsToInput(product.priceCents),
      active: product.active !== false,
    };
  }
}
