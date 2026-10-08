import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, Validators } from '@angular/forms';
import { FieldError, validationMessage } from './field-error';

describe('validationMessage', () => {
  it('translates the standard validators', () => {
    expect(validationMessage(null)).toBeNull();
    expect(validationMessage({ required: true })).toBe('Pflichtfeld');
    expect(validationMessage({ email: true })).toContain('E-Mail');
    expect(validationMessage({ maxlength: { requiredLength: 100, actualLength: 101 } })).toBe(
      'Höchstens 100 Zeichen',
    );
    expect(validationMessage({ minlength: { requiredLength: 3, actualLength: 1 } })).toBe(
      'Mindestens 3 Zeichen',
    );
  });

  it('shows server-side messages', () => {
    expect(validationMessage({ server: 'E-Mail bereits vergeben' })).toBe(
      'E-Mail bereits vergeben',
    );
  });
});

@Component({
  imports: [FieldError],
  template: `<app-field-error [control]="control" />`,
})
class Host {
  readonly control = new FormControl('', [Validators.required, Validators.email]);
}

describe('FieldError', () => {
  it('follows the current error of the control', async () => {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    const text = () => (fixture.nativeElement as HTMLElement).textContent?.trim();
    expect(text()).toBe('Pflichtfeld');

    fixture.componentInstance.control.setValue('kein-email');
    await fixture.whenStable();
    expect(text()).toContain('gültige E-Mail-Adresse');

    fixture.componentInstance.control.setValue('zoe@example.test');
    await fixture.whenStable();
    expect(text()).toBe('');
  });
});
