import { Component, input } from '@angular/core';

/** First focusable element: lets keyboard users jump straight to the main content. */
@Component({
  selector: 'app-skip-link',
  template: `<a class="skip-link" [href]="'#' + target()" (click)="skip($event)">Zum Inhalt springen</a>`,
})
export class SkipLink {
  readonly target = input('main-content');

  protected skip(event: Event): void {
    event.preventDefault();
    document.getElementById(this.target())?.focus();
  }
}
