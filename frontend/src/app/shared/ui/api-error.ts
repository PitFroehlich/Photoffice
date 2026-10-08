import { HttpErrorResponse } from '@angular/common/http';

/**
 * User-facing German message for a failed API call. Uses the RFC 9457 problem detail of the backend if present.
 */
export function apiErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const problem = problemDetail(error.error);
    if (problem && typeof problem.detail === 'string' && problem.detail) {
      return problem.detail;
    }
    switch (error.status) {
      case 0:
        return 'Server nicht erreichbar. Bitte später erneut versuchen.';
      case 400:
        return 'Die Eingaben sind ungültig.';
      case 401:
        return 'Bitte melden Sie sich erneut an.';
      case 403:
        return 'Dafür fehlt Ihnen die Berechtigung.';
      case 404:
        return 'Der Eintrag wurde nicht gefunden.';
      case 409:
        return 'Der Eintrag steht im Konflikt mit einem vorhandenen Eintrag.';
      default:
        return 'Es ist ein Fehler aufgetreten. Bitte später erneut versuchen.';
    }
  }
  return 'Es ist ein Fehler aufgetreten. Bitte später erneut versuchen.';
}

/**
 * The problem detail of an error response. Calls without a response body (e.g. DELETE → 204) are requested as
 * text, so their error body arrives as an unparsed JSON string.
 */
function problemDetail(body: unknown): { detail?: unknown; title?: unknown } | null {
  if (typeof body === 'string') {
    try {
      return JSON.parse(body) as { detail?: unknown };
    } catch {
      return null;
    }
  }
  return body as { detail?: unknown; title?: unknown } | null;
}
