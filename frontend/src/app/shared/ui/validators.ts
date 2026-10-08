import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Like Validators.pattern, but on the trimmed value; empty values are left to Validators.required. */
export function trimmedPattern(pattern: RegExp): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = String(control.value ?? '').trim();
    return value === '' || pattern.test(value) ? null : { pattern: { requiredPattern: String(pattern) } };
  };
}
