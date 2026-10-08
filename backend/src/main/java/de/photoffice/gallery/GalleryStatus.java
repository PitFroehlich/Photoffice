package de.photoffice.gallery;

public enum GalleryStatus {

	/** Never published yet. */
	DRAFT,
	/** Visible for customers (unless expired). */
	ONLINE,
	/** Temporarily not visible for customers. */
	OFFLINE

}
