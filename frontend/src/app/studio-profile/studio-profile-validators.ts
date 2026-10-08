import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Country } from '../api/models';

/**
 * Format rules for the studio profile – identical to the backend (StudioProfileData.java, Country.java).
 * Values are checked trimmed, so surrounding spaces don't produce an error. The backend normalises
 * (IBAN/BIC/VAT ID in canonical form, website with https://).
 */
export const studioProfilePatterns = {
  // Studio or company name: at least one letter, e.g. "Foto & Design GmbH", "Studio 21"
  name: /^(?=.*\p{L})[\p{L}0-9][\p{L}0-9 .,'&+()/:-]*$/u,
  // Street name (may contain digits, e.g. "Straße des 17. Juni") followed by a house number: 4, 4a, 1/2, 10-12
  street:
    /^(?=.*\p{L})[\p{L}0-9 .,'-]*[\p{L}.]\s+[0-9]+\s*[a-zA-Z]?(\s*[-/]\s*[0-9]+\s*[a-zA-Z]?)*$/u,
  city: /^\p{L}[\p{L} .'()/-]*$/u,
  email: /^[^\s@]+@[^\s@]+\.[^\s@.]{2,}$/u,
  phone: /^\+?[0-9][0-9 ()/-]{3,28}[0-9]$/u,
  // Domain with optional http(s) scheme, port and path
  website:
    /^(https?:\/\/)?([\p{L}0-9]([\p{L}0-9-]{0,61}[\p{L}0-9])?\.)+\p{L}{2,}(:[0-9]{1,5})?(\/\S*)?$/iu,
  // 8 to 13 digits separated by / - or spaces
  taxNumber: /^(?=(?:\D*\d){8,13}\D*$)[0-9][0-9 /-]*[0-9]$/u,
  vatId:
    /^(DE ?[0-9]{3} ?[0-9]{3} ?[0-9]{3}|ATU ?[0-9]{8}|CHE[- ]?([0-9]{3})\.?([0-9]{3})\.?([0-9]{3})( ?(MWST|TVA|IVA))?)$/iu,
  bic: /^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$/iu,
};

export const postalCodePatterns: Record<Country, RegExp> = {
  DE: /^[0-9]{5}$/u,
  AT: /^[0-9]{4}$/u,
  CH: /^[0-9]{4}$/u,
};

export const countryLabels: Record<Country, string> = {
  DE: 'Deutschland',
  AT: 'Österreich',
  CH: 'Schweiz',
};

export const studioProfileHints = {
  name: "Mindestens ein Buchstabe; erlaubt sind Buchstaben, Ziffern, Leerzeichen und . , ' & + ( ) / : -",
  street: 'Straße mit Hausnummer, z. B. Lindenstraße 4 oder Am Markt 1/2',
  city: "Nur Buchstaben, Leerzeichen und - . ' ( ) /",
  email: 'Bitte eine gültige E-Mail-Adresse eingeben, z. B. kontakt@mein-studio.de',
  phone: 'Nur Ziffern, Leerzeichen und + - / ( ), mindestens 5 Zeichen',
  website: 'Bitte eine Internetadresse eingeben, z. B. www.mein-studio.de',
  taxNumber: '8 bis 13 Ziffern, getrennt durch / - oder Leerzeichen, z. B. 12/345/67890',
  vatId: 'z. B. DE123456789, ATU12345678 oder CHE-123.456.789 MWST',
  iban: 'Bitte eine gültige IBAN eingeben (Prüfsumme), z. B. DE89 3704 0044 0532 0130 00',
  bic: '8 oder 11 Zeichen, z. B. COBADEFFXXX',
};

export const postalCodeHints: Record<Country, string> = {
  DE: '5 Ziffern für Deutschland',
  AT: '4 Ziffern für Österreich',
  CH: '4 Ziffern für die Schweiz',
};

/** IBAN lengths of the countries the studios are in; other countries are only checked structurally. */
const ibanLengths: Record<string, number> = { DE: 22, AT: 20, CH: 21, LI: 21 };

/** Structure, country length and ISO 7064 mod 97 checksum – like StudioProfileData.normalizeIban. */
export function isValidIban(value: string): boolean {
  const iban = value.replace(/\s/g, '').toUpperCase();
  if (!/^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$/.test(iban)) {
    return false;
  }
  const expectedLength = ibanLengths[iban.slice(0, 2)];
  if (expectedLength !== undefined && iban.length !== expectedLength) {
    return false;
  }
  const rearranged = iban.slice(4) + iban.slice(0, 4);
  let remainder = 0;
  for (const char of rearranged) {
    const digits = /[0-9]/.test(char) ? char : String(char.charCodeAt(0) - 55);
    for (const digit of digits) {
      remainder = (remainder * 10 + Number(digit)) % 97;
    }
  }
  return remainder === 1;
}

/** IBAN check on the trimmed value; empty values are left to Validators.required. */
export function ibanValidator(control: AbstractControl): ValidationErrors | null {
  const value = String(control.value ?? '').trim();
  return value === '' || isValidIban(value) ? null : { pattern: { requiredPattern: 'IBAN' } };
}

/** Postal code format depending on the country in a sibling control. */
export function postalCodeValidator(countryControlName: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = String(control.value ?? '').trim();
    const country = control.parent?.get(countryControlName)?.value as Country | undefined;
    const pattern = postalCodePatterns[country ?? 'DE'];
    return value === '' || pattern.test(value)
      ? null
      : { pattern: { requiredPattern: String(pattern) } };
  };
}

/** Required as soon as one of the sibling controls has a value (bank details belong together). */
export function requiredWith(...otherControlNames: string[]): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const othersFilled = otherControlNames.some(
      (name) => String(control.parent?.get(name)?.value ?? '').trim() !== '',
    );
    const value = String(control.value ?? '').trim();
    return othersFilled && value === '' ? { required: true } : null;
  };
}
