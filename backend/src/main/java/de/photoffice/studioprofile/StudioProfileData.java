package de.photoffice.studioprofile;

import java.math.BigInteger;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Editable studio profile fields, normalised (trimmed, empty optional fields become {@code null}, IBAN/BIC/VAT ID
 * in canonical form, website with scheme) and validated. The same rules are applied in the Angular form
 * ({@code studio-profile-validators.ts}); messages are German because they are shown to studio users.
 */
public record StudioProfileData(String displayName, String street, String postalCode, String city, Country country,
		String email, String phone, String website, String taxNumber, String vatId, String accountHolder, String iban,
		String bic) {

	/** Studio or company name: at least one letter, e.g. "Lichtblick Fotografie", "Foto & Design GmbH", "Studio 21". */
	static final Pattern NAME = Pattern.compile("^(?=.*\\p{L})[\\p{L}0-9][\\p{L}0-9 .,'&+()/:-]*$");

	/** Street name (may contain digits, e.g. "Straße des 17. Juni") followed by a house number like 4, 4a, 1/2, 10-12. */
	static final Pattern STREET = Pattern.compile(
			"^(?=.*\\p{L})[\\p{L}0-9 .,'-]*[\\p{L}.]\\s+[0-9]+\\s*[a-zA-Z]?(\\s*[-/]\\s*[0-9]+\\s*[a-zA-Z]?)*$");

	static final Pattern CITY = Pattern.compile("^\\p{L}[\\p{L} .'()/-]*$");

	static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@.]{2,}$");

	static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 ()/-]{3,28}[0-9]$");

	/** Domain with optional http(s) scheme, port and path, e.g. studio.de, https://www.studio.de/kontakt. */
	static final Pattern WEBSITE = Pattern.compile(
			"^(https?://)?([\\p{L}0-9]([\\p{L}0-9-]{0,61}[\\p{L}0-9])?\\.)+\\p{L}{2,}(:[0-9]{1,5})?(/\\S*)?$",
			Pattern.CASE_INSENSITIVE);

	/** German/Austrian tax number: 8 to 13 digits separated by / - or spaces, e.g. 12/345/67890. */
	static final Pattern TAX_NUMBER = Pattern.compile("^(?=(?:\\D*\\d){8,13}\\D*$)[0-9][0-9 /-]*[0-9]$");

	/** VAT ID: DE123456789, ATU12345678, CHE-123.456.789 MWST (spaces allowed, any case). */
	static final Pattern VAT_ID = Pattern.compile(
			"^(DE ?[0-9]{3} ?[0-9]{3} ?[0-9]{3}|ATU ?[0-9]{8}|CHE[- ]?([0-9]{3})\\.?([0-9]{3})\\.?([0-9]{3})( ?(MWST|TVA|IVA))?)$",
			Pattern.CASE_INSENSITIVE);

	/** IBAN structure (without spaces): country code, check digits, 11 to 30 letters/digits. */
	static final Pattern IBAN = Pattern.compile("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$");

	/** IBAN lengths of the countries the studios are in; other countries are only checked structurally. */
	static final Map<String, Integer> IBAN_LENGTHS = Map.of("DE", 22, "AT", 20, "CH", 21, "LI", 21);

	static final Pattern BIC = Pattern.compile("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$", Pattern.CASE_INSENSITIVE);

	public StudioProfileData {
		displayName = check(required(displayName, "Anzeigename"), NAME,
				"Anzeigename: mindestens ein Buchstabe, erlaubt sind Buchstaben, Ziffern, Leerzeichen und . , ' & + ( ) / : -");
		street = check(optional(street), STREET, "Straße: Straße mit Hausnummer, z. B. Lindenstraße 4 oder Am Markt 1/2");
		if (country == null) {
			throw new IllegalArgumentException("Land ist ein Pflichtfeld");
		}
		postalCode = check(optional(postalCode), country.postalCode(), country.postalCodeMessage());
		city = check(optional(city), CITY, "Ort: nur Buchstaben, Leerzeichen und - . ' ( ) /");
		email = check(optional(email), EMAIL, "E-Mail: bitte eine gültige E-Mail-Adresse angeben");
		email = email == null ? null : email.toLowerCase(Locale.ROOT);
		phone = check(optional(phone), PHONE, "Telefon: nur Ziffern, Leerzeichen und + - / ( ), mindestens 5 Zeichen");
		website = normalizeWebsite(check(optional(website), WEBSITE,
				"Website: bitte eine Internetadresse angeben, z. B. www.mein-studio.de"));
		taxNumber = check(optional(taxNumber), TAX_NUMBER,
				"Steuernummer: 8 bis 13 Ziffern, getrennt durch / - oder Leerzeichen, z. B. 12/345/67890");
		vatId = normalizeVatId(check(optional(vatId), VAT_ID,
				"USt-IdNr.: z. B. DE123456789, ATU12345678 oder CHE-123.456.789 MWST"));
		accountHolder = check(optional(accountHolder), NAME,
				"Kontoinhaber: mindestens ein Buchstabe, erlaubt sind Buchstaben, Ziffern, Leerzeichen und . , ' & + ( ) / : -");
		iban = normalizeIban(optional(iban));
		bic = check(optional(bic), BIC, "BIC: 8 oder 11 Zeichen, z. B. COBADEFFXXX");
		bic = bic == null ? null : bic.toUpperCase(Locale.ROOT);
		if ((accountHolder == null) != (iban == null)) {
			throw new IllegalArgumentException("Bankverbindung: Kontoinhaber und IBAN bitte gemeinsam angeben");
		}
		if (bic != null && iban == null) {
			throw new IllegalArgumentException("Bankverbindung: BIC nur zusammen mit einer IBAN angeben");
		}
	}

	/** Without spaces in upper case; structure, country length and ISO 7064 mod 97 checksum are checked. */
	static String normalizeIban(String value) {
		if (value == null) {
			return null;
		}
		String iban = value.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
		Integer expectedLength = iban.length() >= 2 ? IBAN_LENGTHS.get(iban.substring(0, 2)) : null;
		if (!IBAN.matcher(iban).matches() || (expectedLength != null && iban.length() != expectedLength)
				|| !hasValidChecksum(iban)) {
			throw new IllegalArgumentException(
					"IBAN: bitte eine gültige IBAN angeben (Prüfsumme), z. B. DE89 3704 0044 0532 0130 00");
		}
		return iban;
	}

	private static boolean hasValidChecksum(String iban) {
		String rearranged = iban.substring(4) + iban.substring(0, 4);
		StringBuilder digits = new StringBuilder();
		for (char c : rearranged.toCharArray()) {
			digits.append(Character.isDigit(c) ? String.valueOf(c) : String.valueOf(c - 'A' + 10));
		}
		return new BigInteger(digits.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
	}

	/** DE123456789, ATU12345678, CHE-123.456.789 MWST. */
	private static String normalizeVatId(String value) {
		if (value == null) {
			return null;
		}
		String upper = value.toUpperCase(Locale.ROOT);
		Matcher matcher = VAT_ID.matcher(upper);
		if (!matcher.matches()) {
			throw new IllegalStateException("checked before");
		}
		if (upper.startsWith("CHE")) {
			String suffix = matcher.group(6) == null ? "" : " " + matcher.group(6);
			return "CHE-" + matcher.group(2) + "." + matcher.group(3) + "." + matcher.group(4) + suffix;
		}
		return upper.replace(" ", "");
	}

	private static String normalizeWebsite(String value) {
		if (value == null) {
			return null;
		}
		return value.regionMatches(true, 0, "http://", 0, 7) || value.regionMatches(true, 0, "https://", 0, 8)
				? value : "https://" + value;
	}

	private static String required(String value, String field) {
		String normalized = optional(value);
		if (normalized == null) {
			throw new IllegalArgumentException(field + " ist ein Pflichtfeld");
		}
		return normalized;
	}

	private static String optional(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static String check(String value, Pattern pattern, String message) {
		if (value != null && !pattern.matcher(value).matches()) {
			throw new IllegalArgumentException(message);
		}
		return value;
	}

}
