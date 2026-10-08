import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { provideRouter } from '@angular/router';
import { CustomerPage } from '../api/models';
import { ConfirmService, NotificationService } from '../shared/ui';
import { GermanPaginatorIntl } from '../shared/ui/german-paginator-intl';
import { settle } from '../../testing/settle';
import { iconTesting } from '../../testing/test-providers';
import { CustomerList } from './customer-list';

const page = (count: number, total = count): CustomerPage => ({
  items: Array.from({ length: count }, (_, i) => ({
    id: `id-${i}`,
    firstName: `Vorname${i}`,
    lastName: `Nachname${i}`,
    email: `kunde${i}@example.test`,
    city: 'Berlin',
    createdAt: '2026-10-08T10:00:00Z',
    updatedAt: '2026-10-08T10:00:00Z',
  })),
  page: 0,
  size: 25,
  totalElements: total,
});

describe('CustomerList', () => {
  let httpTesting: HttpTestingController;
  const confirm = vi.fn();
  const notifications = { success: vi.fn(), error: vi.fn() };

  beforeEach(async () => {
    confirm.mockReset();
    notifications.success.mockReset();
    await TestBed.configureTestingModule({
      imports: [CustomerList, iconTesting],
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

  afterEach(() => httpTesting.verify());

  const expectList = (): TestRequest =>
    httpTesting.expectOne((request) => request.url === '/api/studio/customers' && request.method === 'GET');

  async function render(first: CustomerPage) {
    const fixture = TestBed.createComponent(CustomerList);
    fixture.detectChanges();
    expectList().flush(first);
    await settle(fixture);
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  it('shows the customers of the first page', async () => {
    const { element } = await render(page(3));

    expect(element.querySelectorAll('tr[mat-row]').length).toBe(3);
    expect(element.querySelector('tr[mat-row] a')?.textContent).toContain('Nachname0, Vorname0');
    expect(element.querySelector('app-page-header')?.textContent).toContain('3 Kunden');
  });

  it('invites to create the first customer when there are none', async () => {
    const { element } = await render(page(0));

    expect(element.querySelector('app-empty-state')?.textContent).toContain('Noch keine Kunden');
  });

  it('searches on the server and starts at the first page', async () => {
    const { fixture, element } = await render(page(3));
    vi.useFakeTimers();
    const input = element.querySelector('app-search-field input') as HTMLInputElement;
    input.value = 'berlin';
    input.dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(300);
    vi.useRealTimers();

    const request = expectList();
    expect(request.request.params.get('search')).toBe('berlin');
    expect(request.request.params.get('page')).toBe('0');
    request.flush(page(0));
    await settle(fixture);

    expect(element.querySelector('app-empty-state')?.textContent).toContain('Keine Treffer');
  });

  it('deletes after confirmation and reloads', async () => {
    const { fixture, element } = await render(page(2));
    confirm.mockResolvedValue(true);

    (element.querySelector('button[aria-label="Nachname0, Vorname0 löschen"]') as HTMLButtonElement).click();
    await settle(fixture);
    httpTesting.expectOne({ method: 'DELETE', url: '/api/studio/customers/id-0' }).flush(null, { status: 204, statusText: 'No Content' });
    await settle(fixture);
    expectList().flush(page(1));
    await settle(fixture);

    expect(notifications.success).toHaveBeenCalledWith('„Vorname0 Nachname0“ wurde gelöscht.');
    expect(element.querySelectorAll('tr[mat-row]').length).toBe(1);
  });

  it('does not delete without confirmation', async () => {
    const { fixture, element } = await render(page(2));
    confirm.mockResolvedValue(false);

    (element.querySelector('button[aria-label="Nachname0, Vorname0 löschen"]') as HTMLButtonElement).click();
    await settle(fixture);

    httpTesting.expectNone({ method: 'DELETE' });
  });
});
