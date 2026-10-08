package de.photoffice.studioprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Rules for legal texts. Same rules as {@code legal-text-validators.ts}.
 */
class LegalTextTests {

	@Test
	void emptyTextsBecomeNull() {
		assertThat(LegalText.normalize(null)).isNull();
		assertThat(LegalText.normalize("")).isNull();
		assertThat(LegalText.normalize(" \n\t\n ")).isNull();
	}

	@Test
	void removesLeadingBlankLinesAndTrailingWhitespaceButKeepsIndentation() {
		assertThat(LegalText.normalize("\r\n\n# AGB\r\n\n- Punkt\n  - Unterpunkt  \n\n")).isEqualTo("# AGB\n\n- Punkt\n  - Unterpunkt");
	}

	@Test
	void keepsHtmlAsText() {
		// Rendering escapes raw HTML – storing it is harmless and keeps what the user typed
		assertThat(LegalText.normalize("<script>alert(1)</script>")).isEqualTo("<script>alert(1)</script>");
	}

	@Test
	void rejectsControlCharacters() {
		assertThatIllegalArgumentException().isThrownBy(() -> LegalText.normalize("AGB\u0000"))
			.withMessage("Der Text enthält unzulässige Steuerzeichen.");
		assertThatIllegalArgumentException().isThrownBy(() -> LegalText.normalize("AGB\u001B[31m"));
		assertThat(LegalText.normalize("A\tB")).isEqualTo("A\tB");
	}

	@Test
	void limitsTheLength() {
		assertThat(LegalText.normalize("a".repeat(50_000))).hasSize(50_000);
		assertThatIllegalArgumentException().isThrownBy(() -> LegalText.normalize("a".repeat(50_001)))
			.withMessage("Der Text darf höchstens 50.000 Zeichen lang sein.");
	}

}
