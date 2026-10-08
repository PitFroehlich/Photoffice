import { Component, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
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
import {
  createDownloadPackage,
  getDownloadPackage,
  getPriceList,
  updateDownloadPackage,
} from '../api/functions';
import { DownloadPackage, DownloadPackageInput, DownloadPackageKind, Product } from '../api/models';
import {
  FieldError,
  LoadingIndicator,
  NotificationService,
  PageHeader,
  apiErrorMessage,
} from '../shared/ui';
import { centsToInput, parsePriceCents, priceValidator } from './money';
import { canEditPriceList, downloadSize, packageKindLabels } from './price-list-labels';

/** Create (/studio/preisliste/pakete/neu) or edit (/studio/preisliste/pakete/:id) a download package. */
@Component({
  selector: 'app-download-package-form',
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
  templateUrl: './download-package-form.html',
  styleUrl: './price-list-form.scss',
})
export class DownloadPackageForm implements OnInit {
  protected readonly hints = priceListHints;
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notifications = inject(NotificationService);

  protected readonly canEdit = canEditPriceList();
  protected readonly packageId = signal<string | undefined>(undefined);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly title = signal('Neues Download-Paket');
  protected readonly kinds = Object.entries(packageKindLabels) as [DownloadPackageKind, string][];
  /** Download variants of the studio – a package is delivered in one of them. */
  protected readonly variants = signal<Product[]>([]);
  protected readonly downloadSize = downloadSize;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: [
      '',
      [Validators.required, Validators.maxLength(100), trimmedPattern(priceListPatterns.name)],
    ],
    kind: ['IMAGE_COUNT' as DownloadPackageKind, Validators.required],
    imageCount: [10, [Validators.required, Validators.min(2), Validators.max(10000)]],
    downloadProductId: ['', Validators.required],
    price: ['', [Validators.required, priceValidator]],
    active: [true],
  });

  /** The number of images only applies to packages with a fixed count. */
  protected readonly kind = toSignal(this.form.controls.kind.valueChanges, {
    initialValue: this.form.controls.kind.value,
  });

  constructor() {
    this.form.controls.kind.valueChanges.pipe(takeUntilDestroyed()).subscribe((kind) => {
      const imageCount = this.form.controls.imageCount;
      if (kind === 'IMAGE_COUNT') {
        imageCount.enable({ emitEvent: false });
      } else {
        imageCount.disable({ emitEvent: false });
      }
    });
  }

  async ngOnInit(): Promise<void> {
    await this.loadVariants();
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      // Preselect the original if there is one, otherwise the first variant
      const variants = this.variants();
      const preferred = variants.find((v) => !v.maxEdgePx) ?? variants[0];
      if (preferred) {
        this.form.controls.downloadProductId.setValue(preferred.id);
      }
      return;
    }
    this.packageId.set(id);
    this.title.set('Download-Paket bearbeiten');
    this.loading.set(true);
    try {
      const downloadPackage = await this.api.invoke(getDownloadPackage, { packageId: id });
      this.form.reset(this.toFormValue(downloadPackage));
      this.title.set(downloadPackage.name);
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
    const id = this.packageId();
    this.saving.set(true);
    try {
      if (id) {
        await this.api.invoke(updateDownloadPackage, { packageId: id, body });
        this.notifications.success(`„${body.name}“ wurde gespeichert.`);
      } else {
        await this.api.invoke(createDownloadPackage, { body });
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

  private async loadVariants(): Promise<void> {
    try {
      const priceList = await this.api.invoke(getPriceList);
      this.variants.set(priceList.products.filter((p) => p.type === 'DOWNLOAD'));
    } catch (error) {
      this.notifications.error(error);
    }
  }

  private toInput(): DownloadPackageInput {
    const value = this.form.getRawValue();
    return {
      name: value.name.trim(),
      kind: value.kind,
      imageCount: value.kind === 'IMAGE_COUNT' ? value.imageCount : undefined,
      downloadProductId: value.downloadProductId,
      priceCents: parsePriceCents(value.price)!,
      active: value.active,
    };
  }

  private toFormValue(downloadPackage: DownloadPackage) {
    return {
      name: downloadPackage.name,
      kind: downloadPackage.kind,
      imageCount: downloadPackage.imageCount ?? 10,
      downloadProductId: downloadPackage.downloadProductId,
      price: centsToInput(downloadPackage.priceCents),
      active: downloadPackage.active !== false,
    };
  }
}
