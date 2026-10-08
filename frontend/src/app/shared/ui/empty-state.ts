import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/**
 * Shown instead of a list/table when there is nothing to show. Optional action via content projection.
 */
@Component({
  selector: 'app-empty-state',
  imports: [MatIconModule],
  template: `
    <mat-icon [svgIcon]="icon()" aria-hidden="true" />
    <p class="title">{{ title() }}</p>
    @if (message()) {
      <p class="message">{{ message() }}</p>
    }
    <ng-content />
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      padding: 3rem 1rem;
      color: var(--mat-sys-on-surface-variant);
    }
    mat-icon {
      width: 48px;
      height: 48px;
      color: var(--mat-sys-outline);
    }
    .title {
      font: var(--mat-sys-title-medium);
      color: var(--mat-sys-on-surface);
      margin: 0.75rem 0 0.25rem;
    }
    .message {
      margin: 0 0 1rem;
    }
  `,
})
export class EmptyState {
  readonly icon = input('inbox');
  readonly title = input.required<string>();
  readonly message = input<string>();
}
