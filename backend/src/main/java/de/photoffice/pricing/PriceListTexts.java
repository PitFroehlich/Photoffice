package de.photoffice.pricing;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Format rules for the texts of the price list. Same rules in the Angular forms ({@code price-list-validators.ts});
 * German messages because they are shown to studio users.
 */
final class PriceListTexts {

	static final Pattern PAPER_TYPE = Pattern.compile("^\\p{L}[\\p{L}0-9 .,'&+()/-]*$");

	static final Pattern NAME = Pattern.compile("^(?=.*\\p{L})[\\p{L}0-9 .,'&+()/:%-]+$");

	/** Width x height in cm, e.g. 13x18, 13 x 18, 10,5×15 cm. */
	static final Pattern DIMENSIONS = Pattern
		.compile("^(\\d{1,3}(?:[.,]\\d)?)\\s*[x×X*]\\s*(\\d{1,3}(?:[.,]\\d)?)\\s*(?i:cm)?$");

	/** DIN A0 to A6, e.g. A4, DIN A4, din a4. */
	static final Pattern DIN = Pattern.compile("^(?:DIN\\s*)?A\\s*([0-6])$", Pattern.CASE_INSENSITIVE);

	private PriceListTexts() {
	}

	static String paperType(String value) {
		return check(value, PAPER_TYPE,
				"Papiertyp: beginnt mit einem Buchstaben, erlaubt sind Buchstaben, Ziffern, Leerzeichen und . , ' & + ( ) / -");
	}

	static String name(String value, String label) {
		return check(value, NAME, label
				+ ": mindestens ein Buchstabe, erlaubt sind Buchstaben, Ziffern, Leerzeichen und . , ' & + ( ) / : % -");
	}

	/**
	 * Normalises a print format so that equal formats are stored equally: "13 × 18 cm", "10,5 × 15 cm", "DIN A4".
	 */
	static String printFormat(String value) {
		Matcher din = DIN.matcher(value);
		if (din.matches()) {
			return "DIN A" + din.group(1);
		}
		Matcher dimensions = DIMENSIONS.matcher(value);
		if (dimensions.matches()) {
			String width = centimetres(dimensions.group(1));
			String height = centimetres(dimensions.group(2));
			if (width != null && height != null) {
				return width + " × " + height + " cm";
			}
		}
		throw new IllegalArgumentException(
				"Format: Breite x Höhe in cm (z. B. 13 x 18 oder 10,5 x 15) oder DIN A0 bis A6");
	}

	private static String centimetres(String value) {
		BigDecimal number = new BigDecimal(value.replace(',', '.')).stripTrailingZeros();
		if (number.signum() <= 0) {
			return null;
		}
		return number.toPlainString().replace('.', ',');
	}

	private static String check(String value, Pattern pattern, String message) {
		if (value != null && !pattern.matcher(value).matches()) {
			throw new IllegalArgumentException(message);
		}
		return value;
	}

}
