import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import {
  EnvironmentProviders,
  LOCALE_ID,
  inject,
  makeEnvironmentProviders,
  provideAppInitializer,
} from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

registerLocaleData(localeDe);

/**
 * App-wide UI defaults (root): German locale and icons.
 * Icons are the Material Symbols (outlined) as single SVG files under /icons – only used icons are loaded.
 * Usage: <mat-icon svgIcon="delete" />  (name = file name in @material-symbols/svg-400/outlined)
 */
export function provideUiDefaults(): EnvironmentProviders {
  return makeEnvironmentProviders([
    { provide: LOCALE_ID, useValue: 'de-DE' },
    provideAppInitializer(() => {
      const sanitizer = inject(DomSanitizer);
      inject(MatIconRegistry).addSvgIconResolver((name) =>
        sanitizer.bypassSecurityTrustResourceUrl(`icons/${name}.svg`),
      );
    }),
  ]);
}
