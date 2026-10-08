package de.photoffice.studioprofile;

import de.photoffice.tenant.TenantId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A legal text of a studio as Markdown. Clients render it without raw HTML and sanitised (see ADR 0011).
 * At most one row per tenant and kind (row-level security on table {@code studio_legal_text}).
 */
@Entity
@Table(name = "studio_legal_text")
class LegalText {

	static final int MAX_LENGTH = 50_000;

	/** Control characters other than tab and line breaks (PostgreSQL can't store NUL, the others are invisible). */
	static final Pattern FORBIDDEN_CHARACTERS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

	@Id
	private UUID id;

	@Column(name = "tenant_id", nullable = false, updatable = false)
	private UUID tenantId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private LegalTextKind kind;

	@Column(nullable = false)
	private String markdown;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected LegalText() {
	}

	LegalText(TenantId tenantId, LegalTextKind kind) {
		this.id = UUID.randomUUID();
		this.tenantId = tenantId.value();
		this.kind = kind;
	}

	void change(String markdown, Instant now) {
		this.markdown = markdown;
		this.updatedAt = now;
	}

	/**
	 * Trailing whitespace removed, line breaks unified; {@code null} for an empty text. Leading spaces are kept
	 * (they are meaningful in Markdown, e.g. for nested lists).
	 */
	static String normalize(String markdown) {
		if (markdown == null || markdown.isBlank()) {
			return null;
		}
		String normalized = markdown.replace("\r\n", "\n").replace('\r', '\n').stripTrailing();
		while (normalized.startsWith("\n")) {
			normalized = normalized.substring(1);
		}
		if (FORBIDDEN_CHARACTERS.matcher(normalized).find()) {
			throw new IllegalArgumentException("Der Text enthält unzulässige Steuerzeichen.");
		}
		if (normalized.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Der Text darf höchstens 50.000 Zeichen lang sein.");
		}
		return normalized;
	}

	LegalTextKind kind() {
		return kind;
	}

	String markdown() {
		return markdown;
	}

	Instant updatedAt() {
		return updatedAt;
	}

}
