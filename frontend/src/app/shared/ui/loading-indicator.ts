import { Component, input } from '@angular/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';

/** Thin progress bar while data is loading; announced to screen readers. */
@Component({
  selector: 'app-loading-indicator',
  imports: [MatProgressBarModule],
  template: `<mat-progress-bar mode="indeterminate" [attr.aria-label]="label()" />`,
  styles: `
    :host {
      display: block;
      margin: 0.5rem 0;
    }
  `,
})
export class LoadingIndicator {
  readonly label = input('Wird geladen …');
}
