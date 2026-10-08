package de.photoffice.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PriceListTextsTests {

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"13x18 | 13 × 18 cm", "13 x 18 | 13 × 18 cm", "13 X 18 cm | 13 × 18 cm", "13×18cm | 13 × 18 cm",
			"13*18 | 13 × 18 cm", "13 × 18 CM | 13 × 18 cm", "10,5 x 15 | 10,5 × 15 cm", "10.5x15 | 10,5 × 15 cm", "20,0 x 30 | 20 × 30 cm",
			"A4 | DIN A4", "din a4 | DIN A4", "DIN A3 | DIN A3", "DIN  A 0 | DIN A0" })
	void normalisesPrintFormats(String input, String expected) {
		assertThat(PriceListTexts.printFormat(input)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = { "13", "groß", "13 x", "x 18", "13 x 18 x 5", "0 x 18", "1000 x 20", "13 x 18 mm",
			"A7", "DIN B4", "13 / 18", "<script>" })
	void rejectsInvalidPrintFormats(String input) {
		assertThatIllegalArgumentException().isThrownBy(() -> PriceListTexts.printFormat(input))
			.withMessageStartingWith("Format:");
	}

	@ParameterizedTest
	@ValueSource(strings = { "Glänzend", "Matt", "Fine Art Baryt", "Hahnemühle Photo Rag 308", "Seidenmatt (Lustre)",
			"Metallic/Perlmutt", "Lustre-Glanz" })
	void acceptsPaperTypes(String input) {
		assertThat(PriceListTexts.paperType(input)).isEqualTo(input);
	}

	@ParameterizedTest
	@ValueSource(strings = { "308", "!!!", "-Matt", "Matt#1", "<b>Matt</b>" })
	void rejectsPaperTypes(String input) {
		assertThatIllegalArgumentException().isThrownBy(() -> PriceListTexts.paperType(input))
			.withMessageStartingWith("Papiertyp:");
	}

	@ParameterizedTest
	@ValueSource(strings = { "10 Downloads", "Ganze Galerie", "DHL Paket", "Abholung im Studio", "Express (24 h)",
			"Versand: Standard", "50% Rabatt-Paket" })
	void acceptsNames(String input) {
		assertThat(PriceListTexts.name(input, "Name")).isEqualTo(input);
	}

	@ParameterizedTest
	@ValueSource(strings = { "123", "!!!", "10 #Downloads", "<script>", "€€€" })
	void rejectsNames(String input) {
		assertThatIllegalArgumentException().isThrownBy(() -> PriceListTexts.name(input, "Name des Pakets"))
			.withMessageStartingWith("Name des Pakets:");
	}

}
