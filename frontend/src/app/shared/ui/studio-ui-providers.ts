import { Provider } from '@angular/core';
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
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline', subscriptSizing: 'dynamic' } },
  ];
}
