import {
  Component,
  SecurityContext,
  ViewEncapsulation,
  computed,
  inject,
  input,
} from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { renderMarkdown } from './markdown';

/**
 * Shows Markdown written by a studio (e.g. legal texts) safely: raw HTML is escaped by `renderMarkdown`, the
 * resulting HTML goes through Angular's sanitizer (removes script, event handlers, `javascript:` links).
 *
 * <app-markdown-view [markdown]="text()" />
 */
@Component({
  selector: 'app-markdown-view',
  template: `<div class="markdown-view" [innerHTML]="html()"></div>`,
  // Styles must reach the generated elements (h1, ul, table, …)
  encapsulation: ViewEncapsulation.None,
  styles: `
    .markdown-view {
      font: var(--mat-sys-body-large);
      overflow-wrap: anywhere;
    }
    .markdown-view h1 {
      font: var(--mat-sys-headline-small);
      margin: 0 0 1rem;
    }
    .markdown-view h2 {
      font: var(--mat-sys-title-large);
      margin: 1.5rem 0 0.5rem;
    }
    .markdown-view h3,
    .markdown-view h4 {
      font: var(--mat-sys-title-medium);
      margin: 1rem 0 0.5rem;
    }
    .markdown-view p,
    .markdown-view ul,
    .markdown-view ol {
      margin: 0 0 0.75rem;
    }
    .markdown-view a {
      color: var(--mat-sys-primary);
    }
    .markdown-view blockquote {
      margin: 0 0 0.75rem;
      padding-left: 1rem;
      border-left: 3px solid var(--mat-sys-outline-variant);
      color: var(--mat-sys-on-surface-variant);
    }
    .markdown-view table {
      border-collapse: collapse;
      margin-bottom: 0.75rem;
    }
    .markdown-view th,
    .markdown-view td {
      border: 1px solid var(--mat-sys-outline-variant);
      padding: 0.25rem 0.5rem;
      text-align: left;
    }
  `,
})
export class MarkdownView {
  private readonly sanitizer = inject(DomSanitizer);

  readonly markdown = input.required<string>();

  protected readonly html = computed(
    () => this.sanitizer.sanitize(SecurityContext.HTML, renderMarkdown(this.markdown())) ?? '',
  );
}
