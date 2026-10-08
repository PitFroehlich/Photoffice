import { TestBed } from '@angular/core/testing';
import { MAT_DATE_LOCALE } from '@angular/material/core';
import { GermanDateAdapter, formatApiDate, fromApiDate, toApiDate } from './german-date-adapter';

describe('GermanDateAdapter', () => {
  let adapter: GermanDateAdapter;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GermanDateAdapter, { provide: MAT_DATE_LOCALE, useValue: 'de-DE' }],
    });
    adapter = TestBed.inject(GermanDateAdapter);
  });

  it('parses German dates', () => {
    expect(toApiDate(adapter.parse('31.12.2026'))).toBe('2026-12-31');
    expect(toApiDate(adapter.parse('1.2.27'))).toBe('2027-02-01');
    expect(adapter.parse('')).toBeNull();
  });

  it('rejects invalid dates', () => {
    expect(adapter.isValid(adapter.parse('31.02.2026') as Date)).toBe(false);
    expect(adapter.isValid(adapter.parse('2026-12-31x') as Date)).toBe(false);
  });

  it('formats dd.MM.yyyy and converts API dates without time zone shift', () => {
    expect(adapter.format(new Date(2026, 0, 5))).toBe('05.01.2026');
    expect(toApiDate(fromApiDate('2026-03-29'))).toBe('2026-03-29');
    expect(formatApiDate('2026-12-31')).toBe('31.12.2026');
  });
});
