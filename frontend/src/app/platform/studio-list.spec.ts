import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { Router, provideRouter } from '@angular/router';
import { TenantPage, TenantResponse } from '../api/models';
import { ConfirmService, NotificationService } from '../shared/ui';
import { GermanPaginatorIntl } from '../shared/ui/german-paginator-intl';
import { settle } from '../../testing/settle';
import { iconTesting } from '../../testing/test-providers';
import { ONBOARDING_POLL_INTERVAL_MS, StudioList } from './studio-list';

const studio = (overrides: Partial<TenantResponse> = {}): TenantResponse => ({
  id: 'id-a',
  slug: 'studio-a',
  name: 'Studio A',
  status: 'ACTIVE',
  onboardingStatus: 'COMPLETED',
  createdAt: '2026-10-08T10:00:00Z',
  ...overrides,
});

const failed = studio({
  id: 'id-f',
  slug: 'studio-f',
  name: 'Studio F',
  onboardingStatus: 'PENDING',
  onboardingError:
    'Die E-Mail-Adresse admin@studio-a.test gehört bereits zu Studio „Studio A“ (studio-a).',
  onboardingFailedAt: '2026-10-08T10:05:00Z',
});

const isFailed = (s: TenantResponse) => s.onboardingStatus === 'PENDING' && !!s.onboardingError;

const pageOf = (items: TenantResponse[], overrides: Partial<TenantPage> = {}): TenantPage => ({
  items,
  page: 0,
  size: 25,
  totalElements: items.length,
  failedOnboardings: items.filter(isFailed).length,
  ...overrides,
});

describe('StudioList', () => {
  let httpTesting: HttpTestingController;
  const confirm = vi.fn();
  const notifications = { success: vi.fn(), error: vi.fn() };

  beforeEach(async () => {
    confirm.mockReset();
    notifications.success.mockReset();
    notifications.error.mockReset();
    await TestBed.configureTestingModule({
      imports: [StudioList, iconTesting],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ConfirmService, useValue: { confirm } },
        { provide: NotificationService, useValue: notifications },
        { provide: MatPaginatorIntl, useClass: GermanPaginatorIntl },
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    vi.useRealTimers();
    httpTesting.verify();
  });

  const expectList = (): TestRequest =>
    httpTesting.expectOne(
      (request) => request.url === '/api/platform/tenants' && request.method === 'GET',
    );

  const flushList = (studios: TenantResponse[], overrides: Partial<TenantPage> = {}) =>
    expectList().flush(pageOf(studios, overrides));

  async function render(studios: TenantResponse[], overrides: Partial<TenantPage> = {}) {
    const fixture = TestBed.createComponent(StudioList);
    fixture.detectChanges();
    flushList(studios, overrides);
    await settle(fixture);
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  const row = (element: HTMLElement, slug: string) =>
    element.querySelector(`tr[data-slug="${slug}"]`) as HTMLElement;

  it('shows name, slug, status, onboarding status and creation date', async () => {
    const { element } = await render([
      studio(),
      studio({ id: 'id-b', slug: 'studio-b', name: 'Studio B', status: 'SUSPENDED' }),
    ]);

    const a = row(element, 'studio-a');
    expect(a.textContent).toContain('Studio A');
    expect(a.textContent).toContain('studio-a');
    expect(a.textContent).toContain('Aktiv');
    expect(a.textContent).toContain('Abgeschlossen');
    expect(a.textContent).toContain('08.10.2026');
    expect(row(element, 'studio-b').textContent).toContain('Gesperrt');
    expect(element.querySelector('app-page-header')?.textContent).toContain('2 Studios');
  });

  it('shows a failed onboarding with its reason and offers a retry', async () => {
    const { fixture, element } = await render([studio(), failed]);

    const f = row(element, 'studio-f');
    expect(f.textContent).toContain('Fehlgeschlagen');
    expect(f.textContent).toContain('gehört bereits zu Studio „Studio A“');
    expect(element.querySelector('app-page-header')?.textContent).toContain(
      'davon 1 mit fehlgeschlagenem Onboarding',
    );
    expect(
      row(element, 'studio-a').querySelector('button[aria-label*="erneut versuchen"]'),
    ).toBeNull();

    (
      f.querySelector(
        'button[aria-label="Onboarding von Studio F erneut versuchen"]',
      ) as HTMLButtonElement
    ).click();
    await settle(fixture);
    httpTesting
      .expectOne({ method: 'POST', url: '/api/platform/tenants/id-f/onboarding/retry' })
      .flush(failed);
    await settle(fixture);
    flushList([studio(), { ...failed, onboardingStatus: 'COMPLETED', onboardingError: undefined }]);
    await settle(fixture);

    expect(notifications.success).toHaveBeenCalledWith(
      'Das Onboarding von „Studio F“ wird erneut versucht.',
    );
    expect(row(element, 'studio-f').textContent).toContain('Abgeschlossen');
  });

  it('links every studio to its edit page, failed onboardings also from the message', async () => {
    const { element } = await render([studio(), failed]);

    const edit = row(element, 'studio-a').querySelector(
      'a[aria-label="Studio A bearbeiten"]',
    ) as HTMLAnchorElement;
    expect(edit.getAttribute('href')).toBe('/id-a');
    expect(row(element, 'studio-a').querySelector('.correct-link')).toBeNull();

    const correct = row(element, 'studio-f').querySelector('.correct-link') as HTMLAnchorElement;
    expect(correct.textContent).toContain('Daten korrigieren');
    expect(correct.getAttribute('href')).toBe('/id-f');
  });

  it('shows a running onboarding and reloads until it is complete', async () => {
    // Fake timers that still advance in real time, so that settle() keeps working
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const { fixture, element } = await render([studio({ onboardingStatus: 'PENDING' })]);
    expect(row(element, 'studio-a').textContent).toContain('Läuft');
    httpTesting.expectNone({ method: 'GET' });

    vi.advanceTimersByTime(ONBOARDING_POLL_INTERVAL_MS);
    flushList([studio()]);
    await settle(fixture);

    expect(row(element, 'studio-a').textContent).toContain('Abgeschlossen');
  });

  it('suspends a studio after confirmation', async () => {
    const { fixture, element } = await render([studio()]);
    confirm.mockResolvedValue(true);

    (element.querySelector('button[aria-label="Studio A sperren"]') as HTMLButtonElement).click();
    await settle(fixture);
    expect(confirm).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'Studio sperren?', destructive: true }),
    );
    httpTesting
      .expectOne({ method: 'POST', url: '/api/platform/tenants/id-a/suspend' })
      .flush(studio({ status: 'SUSPENDED' }));
    await settle(fixture);
    flushList([studio({ status: 'SUSPENDED' })]);
    await settle(fixture);

    expect(notifications.success).toHaveBeenCalledWith('„Studio A“ ist gesperrt.');
    expect(row(element, 'studio-a').textContent).toContain('Gesperrt');
    expect(element.querySelector('button[aria-label="Studio A freischalten"]')).not.toBeNull();
  });

  it('does not suspend without confirmation', async () => {
    const { fixture, element } = await render([studio()]);
    confirm.mockResolvedValue(false);

    (element.querySelector('button[aria-label="Studio A sperren"]') as HTMLButtonElement).click();
    await settle(fixture);

    httpTesting.expectNone({ method: 'POST' });
  });

  it('reactivates a suspended studio after confirmation', async () => {
    const { fixture, element } = await render([studio({ status: 'SUSPENDED' })]);
    confirm.mockResolvedValue(true);

    (
      element.querySelector('button[aria-label="Studio A freischalten"]') as HTMLButtonElement
    ).click();
    await settle(fixture);
    httpTesting
      .expectOne({ method: 'POST', url: '/api/platform/tenants/id-a/reactivate' })
      .flush(studio());
    await settle(fixture);
    flushList([studio()]);
    await settle(fixture);

    expect(notifications.success).toHaveBeenCalledWith('„Studio A“ ist wieder freigeschaltet.');
  });

  it('reports a failed action', async () => {
    const { fixture, element } = await render([studio()]);
    confirm.mockResolvedValue(true);

    (element.querySelector('button[aria-label="Studio A sperren"]') as HTMLButtonElement).click();
    await settle(fixture);
    httpTesting
      .expectOne({ method: 'POST', url: '/api/platform/tenants/id-a/suspend' })
      .flush(
        { status: 404, detail: 'Kein Studio mit der ID id-a gefunden.' },
        { status: 404, statusText: 'Not Found' },
      );
    await settle(fixture);

    expect(notifications.error).toHaveBeenCalled();
    expect(notifications.success).not.toHaveBeenCalled();
  });

  it('searches on the server, starts at the first page and keeps the state in the URL', async () => {
    const { fixture, element } = await render([studio()], { totalElements: 60 });
    // Go to the second page first
    (
      element.querySelector('button.mat-mdc-paginator-navigation-next') as HTMLButtonElement
    ).click();
    const second = expectList();
    expect(second.request.params.get('page')).toBe('1');
    second.flush(pageOf([studio()], { page: 1, totalElements: 60 }));
    await settle(fixture);

    vi.useFakeTimers({ shouldAdvanceTime: true });
    const input = element.querySelector('app-search-field input') as HTMLInputElement;
    input.value = 'Müller';
    input.dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(300);

    const request = expectList();
    expect(request.request.params.get('search')).toBe('Müller');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('25');
    request.flush(pageOf([], { failedOnboardings: 2 }));
    await settle(fixture);

    expect(TestBed.inject(Router).url).toBe('/?suche=M%C3%BCller');
    expect(element.querySelector('app-empty-state')?.textContent).toContain('Keine Treffer');
    expect(element.querySelector('app-page-header')?.textContent).toContain(
      '0 Treffer für „Müller“ (insgesamt 2 mit fehlgeschlagenem Onboarding)',
    );
  });

  it('shows only failed onboardings on request', async () => {
    const { fixture, element } = await render([studio(), failed]);

    (element.querySelector('button.show-failed') as HTMLButtonElement).click();
    const request = expectList();
    expect(request.request.params.get('onboarding')).toBe('FAILED');
    request.flush(pageOf([failed]));
    await settle(fixture);

    expect(TestBed.inject(Router).url).toBe('/?onboarding=fehlgeschlagen');
    expect(element.querySelectorAll('tr[mat-row]').length).toBe(1);
    expect(element.querySelector('button.show-failed')).toBeNull();
    expect(element.querySelector('app-page-header')?.textContent).toContain('1 Studio gefunden');
  });

  it('restores search, filters and page from the URL', async () => {
    await TestBed.inject(Router).navigateByUrl(
      '/?suche=studio&status=gesperrt&onboarding=offen&seite=2&anzahl=10',
    );
    const fixture = TestBed.createComponent(StudioList);
    fixture.detectChanges();

    const request = expectList();
    expect(request.request.params.get('search')).toBe('studio');
    expect(request.request.params.get('status')).toBe('SUSPENDED');
    expect(request.request.params.get('onboarding')).toBe('PENDING');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('10');
    request.flush(
      pageOf([studio({ status: 'SUSPENDED' })], { page: 2, size: 10, totalElements: 21 }),
    );
    await settle(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect((element.querySelector('app-search-field input') as HTMLInputElement).value).toBe(
      'studio',
    );
    expect(element.querySelector('.mat-mdc-paginator-range-label')?.textContent).toContain(
      '21 – 21 von 21',
    );
  });

  it('goes to the last existing page if the requested one is empty', async () => {
    await TestBed.inject(Router).navigateByUrl('/?seite=5');
    const fixture = TestBed.createComponent(StudioList);
    fixture.detectChanges();

    expectList().flush(pageOf([], { page: 5, totalElements: 30 }));
    await settle(fixture);
    const request = expectList();
    expect(request.request.params.get('page')).toBe('1');
    request.flush(pageOf([studio()], { page: 1, totalElements: 30 }));
    await settle(fixture);

    expect(TestBed.inject(Router).url).toBe('/?seite=1');
  });

  it('invites to register the first studio when there are none', async () => {
    const { element } = await render([]);

    expect(element.querySelector('app-empty-state')?.textContent).toContain('Noch keine Studios');
  });
});
