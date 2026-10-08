package de.photoffice.studioprofile;

import java.time.Instant;
import java.util.Optional;

/**
 * Read model of a legal text for other modules (e.g. customer gallery view #12, checkout #14).
 *
 * @param markdown Markdown source, empty if the studio has not written this text yet
 * @param updatedAt empty if the text was never saved
 */
public record LegalTextContent(LegalTextKind kind, String markdown, Optional<Instant> updatedAt) {

	public boolean isEmpty() {
		return markdown.isEmpty();
	}

}
