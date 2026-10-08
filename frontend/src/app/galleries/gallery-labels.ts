import { Gallery, GalleryStatus } from '../api/models';

export const galleryStatusLabels: Record<GalleryStatus, string> = {
  DRAFT: 'Entwurf',
  ONLINE: 'Online',
  OFFLINE: 'Offline',
};

/** What customers experience: an online gallery past its expiry date is "Abgelaufen". */
export function galleryStateLabel(gallery: Pick<Gallery, 'status' | 'expired'>): string {
  return gallery.status === 'ONLINE' && gallery.expired
    ? 'Abgelaufen'
    : galleryStatusLabels[gallery.status];
}

/** CSS modifier for the status chip. */
export function galleryStateClass(gallery: Pick<Gallery, 'status' | 'expired'>): string {
  if (gallery.status === 'ONLINE' && gallery.expired) {
    return 'expired';
  }
  return gallery.status.toLowerCase();
}

export function customerNames(gallery: Pick<Gallery, 'customers'>): string {
  return gallery.customers.map((c) => `${c.firstName} ${c.lastName}`).join(', ');
}
