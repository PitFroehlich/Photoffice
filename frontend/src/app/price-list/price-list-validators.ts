/**
 * Format rules for price list texts – identical to the backend (PriceListTexts.java).
 * The backend normalises print formats ("13x18" → "13 × 18 cm", "a4" → "DIN A4").
 */
export const priceListPatterns = {
  paperType: /^\p{L}[\p{L}0-9 .,'&+()/-]*$/u,
  name: /^(?=.*\p{L})[\p{L}0-9 .,'&+()/:%-]+$/u,
  // Width x height in cm (13x18, 10,5 × 15 cm; no zero) or DIN A0–A6 – case-insensitive
  printFormat:
    /^(?:(?:din\s*)?a\s*[0-6]|(?!0+(?:[.,]0)?\s*[x×*])\d{1,3}(?:[.,]\d)?\s*[x×*]\s*(?!0+(?:[.,]0)?\s*(?:cm)?$)\d{1,3}(?:[.,]\d)?\s*(?:cm)?)$/iu,
};

export const priceListHints = {
  paperType: "Beginnt mit einem Buchstaben; Buchstaben, Ziffern, Leerzeichen und . , ' & + ( ) / -",
  name: "Mindestens ein Buchstabe; Buchstaben, Ziffern, Leerzeichen und . , ' & + ( ) / : % -",
  printFormat: 'Breite x Höhe in cm (z. B. 13 x 18 oder 10,5 x 15) oder DIN A0 bis A6',
};
