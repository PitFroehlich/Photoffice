import { Signal, computed, inject } from '@angular/core';
import {
  DownloadPackage,
  DownloadPackageKind,
  DownloadResolution,
  Product,
  ProductType,
} from '../api/models';
import { StudioSession } from '../studio/studio-session';

export const productTypeLabels: Record<ProductType, string> = {
  PRINT: 'Abzug',
  DOWNLOAD: 'Download',
};

export const resolutionLabels: Record<DownloadResolution, string> = {
  WEB: 'Web-Auflösung',
  FULL: 'Volle Auflösung',
};

export const packageKindLabels: Record<DownloadPackageKind, string> = {
  IMAGE_COUNT: 'Feste Anzahl Bilder',
  WHOLE_GALLERY: 'Ganze Galerie',
};

/** Human-readable product name: "Abzug Matt, 13 × 18 cm" or "Download Volle Auflösung". */
export function productLabel(
  product: Pick<Product, 'type' | 'paperType' | 'printFormat' | 'resolution'>,
): string {
  if (product.type === 'PRINT') {
    return `Abzug ${product.paperType}, ${product.printFormat}`;
  }
  return `Download ${product.resolution ? resolutionLabels[product.resolution] : ''}`.trim();
}

/** Content of a package: "10 Bilder" or "Ganze Galerie". */
export function packageContent(
  downloadPackage: Pick<DownloadPackage, 'kind' | 'imageCount'>,
): string {
  return downloadPackage.kind === 'IMAGE_COUNT'
    ? `${downloadPackage.imageCount} Bilder`
    : 'Ganze Galerie';
}

/** Everybody in the studio may read the price list, only studio administrators may change it (#13). */
export function canEditPriceList(): Signal<boolean> {
  const session = inject(StudioSession);
  return computed(() => session.user()?.roles.includes('STUDIO_ADMIN') ?? false);
}
