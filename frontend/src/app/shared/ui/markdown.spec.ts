import { TestBed } from '@angular/core/testing';
import { renderMarkdown } from './markdown';
import { MarkdownView } from './markdown-view';

describe('renderMarkdown', () => {
  it('renders headings, emphasis, lists, links and tables', () => {
    const html = renderMarkdown(
      '# AGB\n\n**fett** und *kursiv*\n\n1. eins\n2. zwei\n\n[Preise](https://studio.example/preise)\n\n| A | B |\n|---|---|\n| 1 | 2 |',
    );
    expect(html).toContain('<h1>AGB</h1>');
    expect(html).toContain('<strong>fett</strong>');
    expect(html).toContain('<em>kursiv</em>');
    expect(html).toContain('<ol>');
    expect(html).toContain('<a href="https://studio.example/preise">Preise</a>');
    expect(html).toContain('<table>');
  });

  it('keeps single line breaks (addresses in the imprint)', () => {
    expect(renderMarkdown('Studio A\nLindenstraße 4')).toContain('Studio A<br>Lindenstraße 4');
  });

  it('shows raw HTML as text instead of interpreting it', () => {
    const html = renderMarkdown('<script>alert(1)</script>\n\nText mit <b onclick="x()">Tag</b>');
    expect(html).not.toContain('<script');
    expect(html).not.toContain('<b ');
    expect(html).toContain('&lt;script&gt;');
    expect(html).toContain('&lt;b onclick=&quot;x()&quot;&gt;');
  });

  it('shows images as their alt text (no external requests)', () => {
    const html = renderMarkdown('![Logo](https://tracker.example/pixel.png)');
    expect(html).not.toContain('<img');
    expect(html).toContain('Logo');
  });
});

describe('MarkdownView', () => {
  function render(markdown: string): HTMLElement {
    const fixture = TestBed.createComponent(MarkdownView);
    fixture.componentRef.setInput('markdown', markdown);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('renders the Markdown as HTML', () => {
    const element = render('## Widerruf\n\nSie haben das Recht …');
    expect(element.querySelector('h2')?.textContent).toBe('Widerruf');
    expect(element.querySelector('p')?.textContent).toBe('Sie haben das Recht …');
  });

  it('neutralises javascript: links', () => {
    const element = render('[Klick](javascript:alert(1))');
    const link = element.querySelector('a');
    expect(link?.getAttribute('href') ?? '').not.toMatch(/^javascript:/);
    expect(element.querySelector('script')).toBeNull();
  });

  it('shows script tags as text', () => {
    const element = render('<script>alert(1)</script>');
    expect(element.querySelector('script')).toBeNull();
    expect(element.textContent).toContain('<script>alert(1)</script>');
  });
});
