package de.photoffice.gallery;

/**
 * The requested status change is not possible right now. German message, shown to studio users.
 */
public class GalleryStateException extends RuntimeException {

	GalleryStateException(String message) {
		super(message);
	}

}
