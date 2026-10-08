package de.photoffice.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CustomerDataTests {

	private static CustomerData with(String field, String value) {
		return new CustomerData(field.equals("firstName") ? value : "Julia", field.equals("lastName") ? value : "Becker",
				field.equals("email") ? value : "julia@example.test", field.equals("phone") ? value : null,
				field.equals("street") ? value : null, field.equals("postalCode") ? value : null,
				field.equals("city") ? value : null, null);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"firstName | Anne-Marie", "firstName | Zoë", "firstName | D'Angelo", "lastName | von der Heide",
			"lastName | Müller-Lüdenscheidt", "email | a.b+c@sub.example.de", "phone | 0171 1234567",
			"phone | +49 (30) 123-456", "phone | 030/555123", "street | Lindenstraße 4", "street | Lindenstraße 4a",
			"street | Lindenstraße 4 a", "street | Am Markt 1/2", "street | Lange Reihe 10-12", "street | Hauptstr. 12",
			"street | Hauptstraße 5/3/12", "street | Straße des 17. Juni 135",
			"postalCode | 10969", "postalCode | 1010", "city | Frankfurt (Oder)", "city | Bad Homburg v. d. Höhe",
			"city | Saint-Étienne" })
	void acceptsRealisticValues(String field, String value) {
		with(field, value);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"firstName | Julia2", "firstName | 123", "firstName | <script>", "lastName | -Becker",
			"email | julia@example", "email | julia@@example.test", "email | julia example@test.de",
			"phone | abc", "phone | 0171 12x4567", "phone | 12", "phone | ++49 171", "street | 12345",
			"street | Lindenstraße #4", "street | Lindenstraße", "street | Am Markt", "street | 4 Lindenstraße", "postalCode | ABCDE", "postalCode | 123", "postalCode | 123456",
			"city | 10115 Berlin", "city | Berlin!" })
	void rejectsGarbage(String field, String value) {
		assertThatIllegalArgumentException().isThrownBy(() -> with(field, value));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   " })
	void requiredFieldsMustNotBeBlank(String value) {
		assertThatIllegalArgumentException().isThrownBy(() -> with("firstName", value))
			.withMessage("Vorname ist ein Pflichtfeld");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "phone", "street", "postalCode", "city" })
	void optionalFieldsMayBeEmpty(String field) {
		assertThat(with(field, "  ")).isNotNull();
	}

}
