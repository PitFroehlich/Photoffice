import { Component, input } from '@angular/core';

/**
 * Title area of a page. Buttons for the page go into the projected `[actions]` slot.
 *
 * <app-page-header title="Kunden" subtitle="Alle Kunden des Studios">
 *   <button matButton="filled" actions>Neuer Kunde</button>
 * </app-page-header>
 */
@Component({
  selector: 'app-page-header',
  template: `
    <div class="text">
      <h1>{{ title() }}</h1>
      @if (subtitle()) {
        <p class="subtitle">{{ subtitle() }}</p>
      }
    </div>
    <div class="actions"><ng-content select="[actions]" /></div>
  `,
  styles: `
    :host {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-end;
      justify-content: space-between;
      gap: 1rem;
      margin-bottom: 1.5rem;
    }
    h1 {
      font: var(--mat-sys-headline-medium);
      margin: 0;
    }
    .subtitle {
      font: var(--mat-sys-body-medium);
      color: var(--mat-sys-on-surface-variant);
      margin: 0.25rem 0 0;
    }
    .actions {
      display: flex;
      gap: 0.5rem;
    }
  `,
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
}
