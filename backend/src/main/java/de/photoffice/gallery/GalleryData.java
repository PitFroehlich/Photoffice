package de.photoffice.gallery;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Editable gallery fields, normalised and validated. Same name rules in the Angular form
 * ({@code gallery-validators.ts}); German messages because they are shown to studio users.
 */
public record GalleryData(String name, String description, LocalDate expiresOn, Set<UUID> customerIds) {

	/** At least one letter; letters, digits, spaces and . , ' & + ( ) / : % - (e.g. "Hochzeit Becker 2026"). */
	static final Pattern NAME = Pattern.compile("^(?=.*\\p{L})[\\p{L}0-9 .,'&+()/:%-]+$");

	static final int MAX_CUSTOMERS = 50;

	public GalleryData(String name, String description, LocalDate expiresOn, Collection<UUID> customerIds) {
		this(name, description, expiresOn, customerIds == null ? Set.of() : Set.copyOf(customerIds));
	}

	public GalleryData {
		name = name == null ? null : name.strip();
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Der Name der Galerie ist Pflicht.");
		}
		if (name.length() > 100 || !NAME.matcher(name).matches()) {
			throw new IllegalArgumentException(
					"Name der Galerie: mindestens ein Buchstabe, erlaubt sind Buchstaben, Ziffern, Leerzeichen und . , ' & + ( ) / : % -");
		}
		description = description == null || description.isBlank() ? null : description.strip();
		if (description != null && description.length() > 1000) {
			throw new IllegalArgumentException("Die Beschreibung ist höchstens 1.000 Zeichen lang.");
		}
		customerIds = customerIds == null ? Set.of() : Set.copyOf(customerIds);
		if (customerIds.size() > MAX_CUSTOMERS) {
			throw new IllegalArgumentException("Einer Galerie sind höchstens 50 Kunden zugeordnet.");
		}
	}

}
