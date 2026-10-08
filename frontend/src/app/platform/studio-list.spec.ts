import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TenantResponse } from '../api/models';
import { ConfirmService, NotificationService } from '../shared/ui';
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

  async function render(studios: TenantResponse[]) {
    const fixture = TestBed.createComponent(StudioList);
    fixture.detectChanges();
    expectList().flush(studios);
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
    expectList().flush([
      studio(),
      { ...failed, onboardingStatus: 'COMPLETED', onboardingError: undefined },
    ]);
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
    expectList().flush([studio()]);
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
    expectList().flush([studio({ status: 'SUSPENDED' })]);
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
    expectList().flush([studio()]);
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

  it('invites to register the first studio when there are none', async () => {
    const { element } = await render([]);

    expect(element.querySelector('app-empty-state')?.textContent).toContain('Noch keine Studios');
  });
});
