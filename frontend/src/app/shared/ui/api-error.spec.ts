import { HttpErrorResponse } from '@angular/common/http';
import { apiErrorMessage } from './api-error';

describe('apiErrorMessage', () => {
  it('uses the problem detail of the backend', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { detail: 'Kürzel bereits vergeben' },
    });
    expect(apiErrorMessage(error)).toBe('Kürzel bereits vergeben');
  });

  it('reads the problem detail of calls requested as text (DELETE without body)', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: JSON.stringify({
        status: 409,
        detail: 'Die Download-Variante „Original“ wird vom Paket „10 Downloads“ verwendet.',
      }),
    });
    expect(apiErrorMessage(error)).toContain('wird vom Paket „10 Downloads“ verwendet');
    expect(apiErrorMessage(new HttpErrorResponse({ status: 409, error: 'kein JSON' }))).toContain(
      'Konflikt',
    );
  });

  it('falls back to a German message per status', () => {
    expect(apiErrorMessage(new HttpErrorResponse({ status: 403 }))).toContain('Berechtigung');
    expect(apiErrorMessage(new HttpErrorResponse({ status: 0 }))).toContain('nicht erreichbar');
    expect(apiErrorMessage(new HttpErrorResponse({ status: 500 }))).toContain('Fehler');
  });

  it('handles unknown errors', () => {
    expect(apiErrorMessage(new Error('boom'))).toContain('Fehler');
  });
});
