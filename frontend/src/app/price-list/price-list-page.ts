import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Api } from '../api/api';
import {
  deleteDownloadPackage,
  deleteProduct,
  deleteShippingMethod,
  getPriceList,
} from '../api/functions';
import { DownloadPackage, PriceList, Product, ShippingMethod } from '../api/models';
import {
  ConfirmService,
  EmptyState,
  LoadingIndicator,
  NotificationService,
  PageHeader,
} from '../shared/ui';
import { formatCents } from './money';
import { canEditPriceList, packageContent, productLabel, downloadSize } from './price-list-labels';

/**
 * Price list of the studio (/studio/preisliste): settings, prints, downloads, download packages and shipping
 * methods. Everybody in the studio sees it; only studio administrators get the edit actions.
 */
@Component({
  selector: 'app-price-list-page',
  imports: [
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    PageHeader,
    EmptyState,
    LoadingIndicator,
  ],
  templateUrl: './price-list-page.html',
  styleUrl: './price-list-page.scss',
})
export class PriceListPage implements OnInit {
  private readonly api = inject(Api);
  private readonly confirmService = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  protected readonly canEdit = canEditPriceList();
  protected readonly priceList = signal<PriceList | undefined>(undefined);
  protected readonly loading = signal(false);

  protected readonly prints = computed(
    () => this.priceList()?.products.filter((p) => p.type === 'PRINT') ?? [],
  );
  protected readonly downloads = computed(
    () => this.priceList()?.products.filter((p) => p.type === 'DOWNLOAD') ?? [],
  );

  private readonly withActions = (columns: string[]) =>
    computed(() => (this.canEdit() ? [...columns, 'actions'] : columns));
  protected readonly printColumns = this.withActions([
    'paperType',
    'printFormat',
    'price',
    'status',
  ]);
  protected readonly downloadColumns = this.withActions([
    'downloadName',
    'size',
    'price',
    'status',
  ]);
  protected readonly packageColumns = this.withActions([
    'name',
    'content',
    'variant',
    'price',
    'status',
  ]);
  protected readonly shippingColumns = this.withActions(['name', 'price', 'status']);

  protected readonly packageContent = packageContent;
  protected readonly productLabel = productLabel;

  ngOnInit(): void {
    void this.load();
  }

  protected readonly downloadSize = downloadSize;

  /** Name of the download variant a package is delivered in. */
  protected variantName(downloadProductId: string): string {
    return this.downloads().find((p) => p.id === downloadProductId)?.downloadName ?? '';
  }

  protected price(cents: number): string {
    return formatCents(cents, this.priceList()?.settings.currency);
  }

  protected async removeProduct(product: Product): Promise<void> {
    await this.remove('Produkt löschen?', productLabel(product), () =>
      this.api.invoke(deleteProduct, { productId: product.id }),
    );
  }

  protected async removePackage(downloadPackage: DownloadPackage): Promise<void> {
    await this.remove('Download-Paket löschen?', downloadPackage.name, () =>
      this.api.invoke(deleteDownloadPackage, { packageId: downloadPackage.id }),
    );
  }

  protected async removeShippingMethod(shippingMethod: ShippingMethod): Promise<void> {
    await this.remove('Versandart löschen?', shippingMethod.name, () =>
      this.api.invoke(deleteShippingMethod, { shippingMethodId: shippingMethod.id }),
    );
  }

  private async remove(
    title: string,
    name: string,
    deleteEntry: () => Promise<unknown>,
  ): Promise<void> {
    const confirmed = await this.confirmService.confirm({
      title,
      message: `„${name}“ wird endgültig aus der Preisliste gelöscht. Wenn Sie den Eintrag nur vorübergehend nicht anbieten möchten, deaktivieren Sie ihn stattdessen.`,
      confirmLabel: 'Löschen',
      destructive: true,
    });
    if (!confirmed) {
      return;
    }
    try {
      await deleteEntry();
      this.notifications.success(`„${name}“ wurde gelöscht.`);
      await this.load();
    } catch (error) {
      this.notifications.error(error);
    }
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    try {
      this.priceList.set(await this.api.invoke(getPriceList));
    } catch (error) {
      this.notifications.error(error);
    } finally {
      this.loading.set(false);
    }
  }
}
