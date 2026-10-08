import { Injectable } from '@angular/core';
import { NativeDateAdapter } from '@angular/material/core';

/**
 * Date adapter for German input: understands typed dates like 31.12.2026 or 1.2.27 (the native adapter only parses
 * ISO/US formats) and displays dd.MM.yyyy.
 */
@Injectable()
export class GermanDateAdapter extends NativeDateAdapter {
  override parse(value: unknown): Date | null {
    if (typeof value === 'string') {
      const match = /^\s*(\d{1,2})\.(\d{1,2})\.(\d{2}|\d{4})\s*$/.exec(value);
      if (!match) {
        return value.trim() ? this.invalid() : null;
      }
      const day = Number(match[1]);
      const month = Number(match[2]) - 1;
      const year = match[3].length === 2 ? 2000 + Number(match[3]) : Number(match[3]);
      const date = new Date(year, month, day);
      // Reject overflow like 31.02.
      return date.getMonth() === month && date.getDate() === day ? date : this.invalid();
    }
    return super.parse(value);
  }

  override format(date: Date): string {
    return formatGermanDate(date);
  }

  override getFirstDayOfWeek(): number {
    return 1;
  }
}

/** API date (yyyy-MM-dd, no time zone) → local Date. */
export function fromApiDate(value: string | null | undefined): Date | null {
  if (!value) {
    return null;
  }
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

/** Local Date → API date (yyyy-MM-dd). Never uses toISOString (that would shift the day in UTC). */
export function toApiDate(date: Date | null | undefined): string | undefined {
  if (!date) {
    return undefined;
  }
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** dd.MM.yyyy */
export function formatGermanDate(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()}`;
}

/** Formats an API date for display: 2026-12-31 → 31.12.2026. */
export function formatApiDate(value: string | null | undefined): string {
  const date = fromApiDate(value);
  return date ? formatGermanDate(date) : '';
}
