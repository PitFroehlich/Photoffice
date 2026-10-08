import { Provider } from '@angular/core';
import {
  DateAdapter,
  ErrorStateMatcher,
  MAT_DATE_FORMATS,
  MAT_DATE_LOCALE,
  MAT_NATIVE_DATE_FORMATS,
  ShowOnDirtyErrorStateMatcher,
} from '@angular/material/core';
import { GermanDateAdapter } from './german-date-adapter';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { GermanPaginatorIntl } from './german-paginator-intl';

/**
 * Defaults for form fields and tables of the studio area. Provided by the lazily loaded StudioShell so that
 * these Material modules are not part of the initial bundle of public pages.
 */
export function provideStudioUiDefaults(): Provider[] {
  return [
    { provide: MatPaginatorIntl, useClass: GermanPaginatorIntl },
    {
      provide: MAT_FORM_FIELD_DEFAULT_OPTIONS,
      useValue: { appearance: 'outline', subscriptSizing: 'dynamic' },
    },
    // Show validation errors while typing (dirty), not only after leaving the field
    { provide: ErrorStateMatcher, useClass: ShowOnDirtyErrorStateMatcher },
    // Datepicker: German input and display (dd.MM.yyyy), week starts on Monday
    { provide: MAT_DATE_LOCALE, useValue: 'de-DE' },
    { provide: DateAdapter, useClass: GermanDateAdapter },
    { provide: MAT_DATE_FORMATS, useValue: MAT_NATIVE_DATE_FORMATS },
  ];
}
