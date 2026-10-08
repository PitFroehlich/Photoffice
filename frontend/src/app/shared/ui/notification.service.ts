import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { apiErrorMessage } from './api-error';

/** Short feedback after user actions ("Gespeichert", errors). */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, 'OK', { duration: 4000, horizontalPosition: 'end', politeness: 'polite' });
  }

  /** Accepts a message or an error from an API call (problem detail is shown). */
  error(messageOrError: string | unknown): void {
    const message = typeof messageOrError === 'string' ? messageOrError : apiErrorMessage(messageOrError);
    this.snackBar.open(message, 'Schließen', {
      duration: 8000,
      horizontalPosition: 'end',
      politeness: 'assertive',
      panelClass: 'snackbar-error',
    });
  }
}
