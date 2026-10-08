import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NotificationService, provideStudioUiDefaults } from '../shared/ui';
import { LegalText } from '../api/models';
import { settle } from '../../testing/settle';
import { iconTesting, studioSessionAs } from '../../testing/test-providers';
import { LegalTextsPage } from './legal-texts-page';

const texts: LegalText[] = [
  {
    kind: 'TERMS_AND_CONDITIONS',
    markdown: '# Allgemeine Geschäftsbedingungen\n\n## 1. Geltungsbereich',
    updatedAt: '2026-10-08T10:15:00Z',
  },
  { kind: 'CANCELLATION_POLICY', markdown: '' },
  { kind: 'IMPRINT', markdown: '' },
  { kind: 'PRIVACY_POLICY', markdown: '' },
];

describe('LegalTextsPage', () => {
  let httpTesting: HttpTestingController;
  const notifications = { success: vi.fn(), error: vi.fn() };

  async function render(role: 'STUDIO_ADMIN' | 'PHOTOGRAPHER' = 'STUDIO_ADMIN') {
    notifications.success.mockClear();
    await TestBed.configureTestingModule({
      imports: [LegalTextsPage, iconTesting],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideStudioUiDefaults(),
        studioSessionAs(role),
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(LegalTextsPage);
    fixture.detectChanges();
    httpTesting.expectOne({ method: 'GET', url: '/api/studio/profile/legal-texts' }).flush(texts);
    httpTesting.expectOne({ method: 'GET', url: '/api/studio/profile' }).flush({
      displayName: 'Studio A Fotografie',
      street: 'Lindenstraße 4',
      postalCode: '10969',
      city: 'Berlin',
      country: 'DE',
      email: 'kontakt@studio-a.example',
      vatId: 'DE123456789',
    });
    await settle(fixture);
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  function textarea(element: HTMLElement): HTMLTextAreaElement {
    return element.querySelector('textarea') as HTMLTextAreaElement;
  }

  function type(element: HTMLElement, value: string) {
    textarea(element).value = value;
    textarea(element).dispatchEvent(new Event('input'));
  }

  function button(element: HTMLElement, label: string): HTMLButtonElement {
    return [...element.querySelectorAll('button')].find((b) => b.textContent?.trim() === label)!;
  }

  async function openTab(
    fixture: { detectChanges(): void; whenStable(): Promise<unknown> },
    element: HTMLElement,
    label: string,
  ) {
    const tab = [...element.querySelectorAll('[role="tab"]')].find((t) =>
      t.textContent?.includes(label),
    ) as HTMLElement;
    tab.click();
    await settle(fixture);
  }

  afterEach(() => httpTesting.verify());

  it('shows the saved text with a live preview', async () => {
    const { fixture, element } = await render();
    expect(textarea(element).value).toContain('# Allgemeine Geschäftsbedingungen');
    expect(element.querySelector('.preview h1')?.textContent).toBe(
      'Allgemeine Geschäftsbedingungen',
    );
    expect(button(element, 'AGB speichern').disabled).toBe(true);

    type(element, '# AGB\n\n**Neu** <script>alert(1)</script>');
    await settle(fixture);
    expect(element.querySelector('.preview h1')?.textContent).toBe('AGB');
    expect(element.querySelector('.preview strong')?.textContent).toBe('Neu');
    expect(element.querySelector('.preview script')).toBeNull();
    expect(element.textContent).toContain('Ungespeicherte Änderungen');
    expect(button(element, 'AGB speichern').disabled).toBe(false);
  });

  it('saves a text', async () => {
    const { fixture, element } = await render();
    type(element, '# AGB\n\nNeu  ');
    await settle(fixture);
    button(element, 'AGB speichern').click();
    await settle(fixture);

    const request = httpTesting.expectOne({
      method: 'PUT',
      url: '/api/studio/profile/legal-texts/TERMS_AND_CONDITIONS',
    });
    expect(request.request.body).toEqual({ markdown: '# AGB\n\nNeu  ' });
    request.flush({
      kind: 'TERMS_AND_CONDITIONS',
      markdown: '# AGB\n\nNeu',
      updatedAt: '2026-10-08T11:00:00Z',
    });
    await settle(fixture);
    expect(notifications.success).toHaveBeenCalledWith(
      '„Allgemeine Geschäftsbedingungen“ wurde gespeichert.',
    );
    expect(textarea(element).value).toBe('# AGB\n\nNeu');
    expect(element.textContent).not.toContain('Ungespeicherte Änderungen');
  });

  it('fills an empty imprint with an outline from the studio profile', async () => {
    const { fixture, element } = await render();
    await openTab(fixture, element, 'Impressum');
    expect(textarea(element).value).toBe('');

    button(element, 'Gliederung einfügen').click();
    await settle(fixture);
    expect(textarea(element).value).toContain(
      '**Studio A Fotografie**\nLindenstraße 4\n10969 Berlin\nDeutschland',
    );
    expect(textarea(element).value).toContain('Umsatzsteuer-ID: DE123456789');
    expect(element.querySelector('.preview strong')?.textContent).toBe('Studio A Fotografie');
  });

  it('shows the rendered texts read-only for photographers', async () => {
    const { fixture, element } = await render('PHOTOGRAPHER');
    expect(textarea(element)).toBeNull();
    expect(element.querySelector('app-markdown-view h1')?.textContent).toBe(
      'Allgemeine Geschäftsbedingungen',
    );
    expect(element.textContent).toContain(
      'Nur Studio-Administratoren können die Rechtstexte ändern.',
    );

    await openTab(fixture, element, 'Widerruf');
    expect(element.textContent).toContain('Widerrufsbelehrung fehlt noch');
  });
});
