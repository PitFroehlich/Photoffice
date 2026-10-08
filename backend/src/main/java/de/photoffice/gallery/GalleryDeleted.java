package de.photoffice.gallery;

import de.photoffice.tenant.TenantId;
import java.util.UUID;

/**
 * A gallery was deleted, e.g. so that its images are removed from storage (#9).
 */
public record GalleryDeleted(TenantId tenantId, UUID galleryId) {
}
