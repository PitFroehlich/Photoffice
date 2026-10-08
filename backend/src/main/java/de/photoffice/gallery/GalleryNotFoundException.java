package de.photoffice.gallery;

public class GalleryNotFoundException extends RuntimeException {

	GalleryNotFoundException() {
		super("Die Galerie wurde nicht gefunden.");
	}

}
