import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Highest price the backend accepts (100.000,00). */
export const MAX_PRICE_CENTS = 10_000_000;

const PRICE_PATTERN = /^(\d{1,6})(?:[.,](\d{1,2}))?$/;

const groupedInteger = new Intl.NumberFormat('de-DE', { maximumFractionDigits: 0 });

/**
 * Parses a price as typed by a German user ("12,90", "12.9", "12", "12,90 €") into whole cents.
 * Pure string arithmetic – never floating point. Returns null for invalid input.
 */
export function parsePriceCents(text: string): number | null {
  const match = PRICE_PATTERN.exec(text.replace('€', '').trim());
  if (!match) {
    return null;
  }
  const cents = Number(match[1]) * 100 + Number((match[2] ?? '0').padEnd(2, '0'));
  return cents <= MAX_PRICE_CENTS ? cents : null;
}

/** Cents as form input value: 1290 → "12,90". */
export function centsToInput(cents: number): string {
  return `${Math.trunc(cents / 100)},${String(cents % 100).padStart(2, '0')}`;
}

/** Cents for display: 129000 → "1.290,00 €". Integer arithmetic only. */
export function formatCents(cents: number, currency = 'EUR'): string {
  const symbol = currency === 'EUR' ? '€' : currency;
  return `${groupedInteger.format(Math.trunc(cents / 100))},${String(cents % 100).padStart(2, '0')} ${symbol}`;
}

/** Form validator for prices; error key "price" (message in FieldError). Empty values are left to `required`. */
export const priceValidator: ValidatorFn = (
  control: AbstractControl<string>,
): ValidationErrors | null => {
  const value = control.value?.trim();
  return !value || parsePriceCents(value) !== null ? null : { price: true };
};
