package de.photoffice.studioprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Format rules of the studio profile. Same cases as {@code studio-profile-validators.spec.ts} – frontend and backend
 * must agree.
 */
class StudioProfileDataTests {

	private static final String HOLDER = "Studio A Fotografie GmbH";

	private static final String IBAN = "DE89370400440532013000";

	private static StudioProfileData with(String field, String value) {
		return with(field, value, Country.DE);
	}

	/** A minimal valid profile with one field set; bank fields get their required partner fields. */
	private static StudioProfileData with(String field, String value, Country country) {
		boolean bank = field.equals("iban") || field.equals("accountHolder") || field.equals("bic");
		return new StudioProfileData(field.equals("displayName") ? value : "Lichtblick Fotografie",
				field.equals("street") ? value : null, field.equals("postalCode") ? value : null,
				field.equals("city") ? value : null, country, field.equals("email") ? value : null,
				field.equals("phone") ? value : null, field.equals("website") ? value : null,
				field.equals("taxNumber") ? value : null, field.equals("vatId") ? value : null,
				field.equals("accountHolder") ? value : bank ? HOLDER : null,
				field.equals("iban") ? value : bank ? IBAN : null, field.equals("bic") ? value : null);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "displayName | Lichtblick Fotografie", "displayName | Foto & Design GmbH",
			"displayName | Studio 21", "displayName | 21 Gramm Fotografie", "displayName | Foto-Atelier Müller",
			"street | Lindenstraße 4", "street | Am Markt 1/2", "street | Straße des 17. Juni 135",
			"city | Frankfurt (Oder)", "city | Zürich", "email | kontakt@studio.example.de",
			"phone | +43 1 2345678", "phone | 030 1234567", "website | studio.de", "website | www.studio-a.de",
			"website | https://www.studio-a.de/kontakt", "website | http://fotografie-müller.de",
			"website | https://studio.de:8443/", "taxNumber | 12/345/67890", "taxNumber | 2181508150",
			"taxNumber | 21 815 08150", "taxNumber | 12-345/6789", "taxNumber | 1121081508150",
			"vatId | DE123456789", "vatId | de 123 456 789", "vatId | ATU12345678", "vatId | CHE-123.456.789 MWST",
			"vatId | CHE123456789", "vatId | che-123.456.789 tva", "accountHolder | Studio A Fotografie GmbH",
			"iban | DE89370400440532013000", "iban | DE89 3704 0044 0532 0130 00",
			"iban | de89 3704 0044 0532 0130 00", "iban | AT611904300234573201", "iban | CH9300762011623852957",
			"bic | COBADEFFXXX", "bic | cobadeff", "bic | GIBAATWW" })
	void acceptsRealisticValues(String field, String value) {
		with(field, value);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "displayName | 123", "displayName | -Studio", "displayName | Studio <b>",
			"displayName | Studio!", "displayName | @home", "street | 12345", "street | Lindenstraße",
			"city | 10115 Berlin", "email | kontakt@studio", "phone | abc", "website | studio",
			"website | ftp://studio.de", "website | https://", "website | studio .de", "website | javascript:alert(1)",
			"taxNumber | 1234567", "taxNumber | 12345678901234", "taxNumber | 12/345/6789a", "taxNumber | /12345678",
			"vatId | DE12345678", "vatId | ATU1234567", "vatId | AT12345678", "vatId | FR12345678901",
			"vatId | CHE-123.456.78", "accountHolder | !!!", "iban | DE89370400440532013001",
			"iban | DE8937040044053201300", "iban | AT611904300234573202", "iban | XX00", "iban | DE89-3704-0044",
			"bic | COBADEF", "bic | COBA1EFFXXX", "bic | COBADEFFXX" })
	void rejectsGarbage(String field, String value) {
		assertThatIllegalArgumentException().isThrownBy(() -> with(field, value));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "DE | 10969", "DE | 04109", "AT | 1060", "CH | 8001" })
	void acceptsPostalCodesOfTheCountry(Country country, String postalCode) {
		with("postalCode", postalCode, country);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "DE | 1060 | PLZ: 5 Ziffern für Deutschland",
			"AT | 10969 | PLZ: 4 Ziffern für Österreich", "CH | ABCD | PLZ: 4 Ziffern für die Schweiz",
			"DE | 123456 | PLZ: 5 Ziffern für Deutschland" })
	void rejectsPostalCodesOfOtherCountries(Country country, String postalCode, String message) {
		assertThatIllegalArgumentException().isThrownBy(() -> with("postalCode", postalCode, country))
			.withMessage(message);
	}

	@Test
	void normalisesValues() {
		StudioProfileData data = new StudioProfileData("  Lichtblick  ", " ", "", null, Country.CH,
				"Kontakt@Studio.CH", null, "www.studio.ch", null, "che123456789 mwst", HOLDER,
				"ch93 0076 2011 6238 5295 7", "ubswchzh80a");
		assertThat(data.displayName()).isEqualTo("Lichtblick");
		assertThat(data.street()).isNull();
		assertThat(data.postalCode()).isNull();
		assertThat(data.email()).isEqualTo("kontakt@studio.ch");
		assertThat(data.website()).isEqualTo("https://www.studio.ch");
		assertThat(data.vatId()).isEqualTo("CHE-123.456.789 MWST");
		assertThat(data.iban()).isEqualTo("CH9300762011623852957");
		assertThat(data.bic()).isEqualTo("UBSWCHZH80A");

		assertThat(with("vatId", "de 123 456 789").vatId()).isEqualTo("DE123456789");
		assertThat(with("website", "HTTP://studio.de").website()).isEqualTo("HTTP://studio.de");
	}

	@Test
	void displayNameAndCountryAreRequired() {
		assertThatIllegalArgumentException().isThrownBy(() -> with("displayName", "  "))
			.withMessage("Anzeigename ist ein Pflichtfeld");
		assertThatIllegalArgumentException().isThrownBy(() -> with("city", "Berlin", null))
			.withMessage("Land ist ein Pflichtfeld");
	}

	@Test
	void bankDetailsBelongTogether() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new StudioProfileData("Studio", null, null, null, Country.DE, null, null, null, null,
					null, HOLDER, null, null))
			.withMessage("Bankverbindung: Kontoinhaber und IBAN bitte gemeinsam angeben");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new StudioProfileData("Studio", null, null, null, Country.DE, null, null, null, null,
					null, null, IBAN, null))
			.withMessage("Bankverbindung: Kontoinhaber und IBAN bitte gemeinsam angeben");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new StudioProfileData("Studio", null, null, null, Country.DE, null, null, null, null,
					null, null, null, "COBADEFFXXX"))
			.withMessage("Bankverbindung: BIC nur zusammen mit einer IBAN angeben");
	}

}
