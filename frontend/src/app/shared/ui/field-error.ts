import { Component, computed, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, ValidationErrors } from '@angular/forms';
import { map, startWith, switchMap } from 'rxjs';

/** German message for the first validation error of a control. */
export function validationMessage(errors: ValidationErrors | null, patternHint?: string): string | null {
  if (!errors) {
    return null;
  }
  if (errors['required']) {
    return 'Pflichtfeld';
  }
  if (errors['email']) {
    return patternHint ?? 'Bitte eine gültige E-Mail-Adresse eingeben';
  }
  if (errors['minlength']) {
    return `Mindestens ${errors['minlength'].requiredLength} Zeichen`;
  }
  if (errors['maxlength']) {
    return `Höchstens ${errors['maxlength'].requiredLength} Zeichen`;
  }
  if (errors['min']) {
    return `Mindestens ${errors['min'].min}`;
  }
  if (errors['max']) {
    return `Höchstens ${errors['max'].max}`;
  }
  if (errors['pattern']) {
    return patternHint ?? 'Ungültiges Format';
  }
  if (errors['server']) {
    return String(errors['server']);
  }
  return 'Ungültige Eingabe';
}

/**
 * Validation message inside <mat-error>:
 * <mat-error><app-field-error [control]="form.controls.email" /></mat-error>
 * Format errors: pass a field-specific hint, e.g. [patternHint]="'4 oder 5 Ziffern'".
 */
@Component({
  selector: 'app-field-error',
  template: `{{ message() }}`,
})
export class FieldError {
  readonly control = input.required<AbstractControl>();
  /** Message for format errors (pattern/email) instead of the generic one. */
  readonly patternHint = input<string>();

  // Follows every validity change of the control (the component is only re-rendered on signal changes)
  private readonly errors = toSignal(
    toObservable(this.control).pipe(
      switchMap((control) => control.statusChanges.pipe(startWith(control.status), map(() => control.errors))),
    ),
    { initialValue: null },
  );

  protected readonly message = computed(() => validationMessage(this.errors(), this.patternHint()));
}
