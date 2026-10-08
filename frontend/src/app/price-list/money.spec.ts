import { FormControl } from '@angular/forms';
import { centsToInput, formatCents, parsePriceCents, priceValidator } from './money';

describe('money', () => {
  it('parses German and English decimal input into whole cents', () => {
    expect(parsePriceCents('12,90')).toBe(1290);
    expect(parsePriceCents('12.9')).toBe(1290);
    expect(parsePriceCents('12')).toBe(1200);
    expect(parsePriceCents(' 0,05 € ')).toBe(5);
    expect(parsePriceCents('0')).toBe(0);
    // Classic floating point trap: 0.29 * 100 = 28.999999999999996
    expect(parsePriceCents('0,29')).toBe(29);
    expect(parsePriceCents('100000')).toBe(10_000_000);
  });

  it('rejects invalid amounts', () => {
    expect(parsePriceCents('')).toBeNull();
    expect(parsePriceCents('-1')).toBeNull();
    expect(parsePriceCents('1,999')).toBeNull();
    expect(parsePriceCents('1.234,50')).toBeNull();
    expect(parsePriceCents('abc')).toBeNull();
    expect(parsePriceCents('100000,01')).toBeNull();
  });

  it('formats cents for display and as input', () => {
    expect(formatCents(1290)).toBe('12,90 €');
    expect(formatCents(5)).toBe('0,05 €');
    expect(formatCents(129000)).toBe('1.290,00 €');
    expect(centsToInput(1290)).toBe('12,90');
    expect(centsToInput(7)).toBe('0,07');
  });

  it('validates price inputs', () => {
    expect(priceValidator(new FormControl('12,90'))).toBeNull();
    expect(priceValidator(new FormControl(''))).toBeNull();
    expect(priceValidator(new FormControl('12,999'))).toEqual({ price: true });
  });
});
