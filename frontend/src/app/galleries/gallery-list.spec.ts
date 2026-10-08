import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { GalleryPage } from '../api/models';
import { ConfirmService, NotificationService, provideStudioUiDefaults } from '../shared/ui';
import { settle } from '../../testing/settle';
import { iconTesting } from '../../testing/test-providers';
import { GalleryList } from './gallery-list';

const page = (): GalleryPage => ({
  items: [
    {
      id: 'g1',
      name: 'Hochzeit Becker',
      status: 'ONLINE',
      expired: false,
      expiresOn: '2026-12-31',
      customers: [
        { id: 'c1', firstName: 'Julia', lastName: 'Becker', email: 'julia@example.test' },
      ],
      createdAt: '2026-10-08T10:00:00Z',
      updatedAt: '2026-10-08T10:00:00Z',
    },
    {
      id: 'g2',
      name: 'Babybauch Schröder',
      status: 'ONLINE',
      expired: true,
      expiresOn: '2026-10-01',
      customers: [],
      createdAt: '2026-10-08T10:00:00Z',
      updatedAt: '2026-10-08T10:00:00Z',
    },
  ],
  page: 0,
  size: 25,
  totalElements: 2,
});

describe('GalleryList', () => {
  let httpTesting: HttpTestingController;
  const confirm = vi.fn();
  const notifications = { success: vi.fn(), error: vi.fn() };

  async function render(query: Record<string, string> = {}) {
    await TestBed.configureTestingModule({
      imports: [GalleryList, iconTesting],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideStudioUiDefaults(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(query) } },
        },
        { provide: ConfirmService, useValue: { confirm } },
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(GalleryList);
    fixture.detectChanges();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  const expectList = (): TestRequest =>
    httpTesting.expectOne(
      (request) => request.url === '/api/studio/galleries' && request.method === 'GET',
    );

  afterEach(() => httpTesting.verify());

  it('shows galleries with customers, state and expiry date', async () => {
    const { fixture, element } = await render();
    expectList().flush(page());
    await settle(fixture);

    const rows = element.querySelectorAll('tr[mat-row]');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('Julia Becker');
    expect(rows[0].textContent).toContain('Online');
    expect(rows[0].textContent).toContain('31.12.2026');
    expect(rows[1].textContent).toContain('Abgelaufen');
    expect(element.querySelector('app-page-header')?.textContent).toContain('2 Galerien');
  });

  it('filters by status on the server', async () => {
    const { fixture, element } = await render();
    expectList().flush(page());
    await settle(fixture);

    (
      Array.from(element.querySelectorAll('mat-button-toggle button')).find((b) =>
        b.textContent?.includes('Entwurf'),
      ) as HTMLButtonElement
    ).click();
    await settle(fixture);

    const request = expectList();
    expect(request.request.params.get('status')).toBe('DRAFT');
    request.flush({ items: [], page: 0, size: 25, totalElements: 0 });
    await settle(fixture);
    expect(element.querySelector('app-empty-state')?.textContent).toContain('Keine Treffer');
  });

  it('takes the customer filter from the URL', async () => {
    const { fixture, element } = await render({ kunde: 'c1' });
    const request = expectList();
    expect(request.request.params.get('customerId')).toBe('c1');
    request.flush(page());
    await settle(fixture);

    expect(element.textContent).toContain('Filter aufheben');
  });
});
