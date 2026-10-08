const dateTime = new Intl.DateTimeFormat('de-DE', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

/**
 * "08.10.2026, 12:15" in the browser's time zone. Intl instead of Angular's DatePipe: the DatePipe would move
 * Angular's date formatting into the shared initial chunk (initial bundle budget).
 */
export function formatDateTime(iso: string): string {
  return dateTime.format(new Date(iso));
}
