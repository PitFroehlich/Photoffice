import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Format rules for customer fields – identical to the backend (CustomerData.java).
 * Values are checked trimmed, so surrounding spaces don't produce an error.
 */
export const customerPatterns = {
  name: /^\p{L}[\p{L} .'-]*$/u,
  email: /^[^\s@]+@[^\s@]+\.[^\s@.]{2,}$/u,
  phone: /^\+?[0-9][0-9 ()/-]{3,28}[0-9]$/u,
  postalCode: /^[0-9]{4,5}$/u,
  city: /^\p{L}[\p{L} .'()/-]*$/u,
  // Street name (may contain digits, e.g. "Straße des 17. Juni") followed by a house number: 4, 4a, 1/2, 10-12
  street: /^(?=.*\p{L})[\p{L}0-9 .,'-]*[\p{L}.]\s+[0-9]+\s*[a-zA-Z]?(\s*[-/]\s*[0-9]+\s*[a-zA-Z]?)*$/u,
};

export const customerHints = {
  name: 'Nur Buchstaben, Leerzeichen, Bindestrich, Apostroph und Punkt',
  email: 'Bitte eine gültige E-Mail-Adresse eingeben, z. B. name@beispiel.de',
  phone: 'Nur Ziffern, Leerzeichen und + - / ( ), mindestens 5 Zeichen',
  postalCode: '4 oder 5 Ziffern',
  city: 'Nur Buchstaben, Leerzeichen und - . \' ( ) /',
  street: 'Straße mit Hausnummer, z. B. Lindenstraße 4 oder Am Markt 1/2',
};

/** Like Validators.pattern, but on the trimmed value; empty values are left to Validators.required. */
export function trimmedPattern(pattern: RegExp): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = String(control.value ?? '').trim();
    return value === '' || pattern.test(value) ? null : { pattern: { requiredPattern: String(pattern) } };
  };
}
