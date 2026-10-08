package de.photoffice.gallery;

import de.photoffice.tenant.TenantId;
import java.util.Set;
import java.util.UUID;

/**
 * A gallery went online, e.g. to send the access link to its customers (#19).
 */
public record GalleryPublished(TenantId tenantId, UUID galleryId, Set<UUID> customerIds) {
}
