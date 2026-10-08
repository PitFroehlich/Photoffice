import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { PriceList } from '../api/models';
import { ConfirmService, NotificationService } from '../shared/ui';
import { settle } from '../../testing/settle';
import { iconTesting, studioSessionAs } from '../../testing/test-providers';
import { PriceListPage } from './price-list-page';

const timestamps = { createdAt: '2026-10-08T10:00:00Z', updatedAt: '2026-10-08T10:00:00Z' };

const priceList = (): PriceList => ({
  settings: { currency: 'EUR', vatRatePercent: 19 },
  products: [
    {
      id: 'p1',
      type: 'PRINT',
      paperType: 'Matt',
      printFormat: '13 × 18 cm',
      priceCents: 290,
      active: true,
      ...timestamps,
    },
    {
      id: 'p2',
      type: 'PRINT',
      paperType: 'Fine Art',
      printFormat: '30 × 45 cm',
      priceCents: 2490,
      active: false,
      ...timestamps,
    },
    {
      id: 'p3',
      type: 'DOWNLOAD',
      resolution: 'FULL',
      priceCents: 990,
      active: true,
      ...timestamps,
    },
  ],
  downloadPackages: [
    {
      id: 'd1',
      name: '10 Downloads',
      kind: 'IMAGE_COUNT',
      imageCount: 10,
      resolution: 'FULL',
      priceCents: 6900,
      active: true,
      ...timestamps,
    },
  ],
  shippingMethods: [],
});

describe('PriceListPage', () => {
  let httpTesting: HttpTestingController;
  const confirm = vi.fn();
  const notifications = { success: vi.fn(), error: vi.fn() };

  async function render(role: 'STUDIO_ADMIN' | 'PHOTOGRAPHER', list = priceList()) {
    confirm.mockReset();
    notifications.success.mockReset();
    await TestBed.configureTestingModule({
      imports: [PriceListPage, iconTesting],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        studioSessionAs(role),
        { provide: ConfirmService, useValue: { confirm } },
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(PriceListPage);
    fixture.detectChanges();
    httpTesting.expectOne({ method: 'GET', url: '/api/studio/price-list' }).flush(list);
    await settle(fixture);
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  const table = (element: HTMLElement, label: string) =>
    element.querySelector(`table[aria-label="${label}"]`);

  afterEach(() => httpTesting.verify());

  it('shows all sections with formatted prices and status', async () => {
    const { element } = await render('STUDIO_ADMIN');

    expect(element.querySelector('[data-testid="vat-rate"]')?.textContent).toContain('19 %');
    const prints = table(element, 'Abzüge')!;
    expect(prints.querySelectorAll('tr[mat-row]').length).toBe(2);
    expect(prints.textContent).toContain('2,90 €');
    expect(prints.textContent).toContain('24,90 €');
    expect(prints.textContent).toContain('Inaktiv');
    expect(table(element, 'Downloads')?.textContent).toContain('Volle Auflösung');
    expect(table(element, 'Download-Pakete')?.textContent).toContain('10 Bilder');
    expect(element.textContent).toContain('Noch keine Versandarten');
  });

  it('offers editing to studio administrators', async () => {
    const { element } = await render('STUDIO_ADMIN');

    expect(element.textContent).toContain('Abzug hinzufügen');
    expect(element.textContent).toContain('Steuersatz ändern');
    expect(
      element.querySelector('button[aria-label="Abzug Matt, 13 × 18 cm löschen"]'),
    ).not.toBeNull();
    expect(element.textContent).not.toContain('Nur Studio-Administratoren');
  });

  it('shows photographers a read-only price list', async () => {
    const { element } = await render('PHOTOGRAPHER');

    expect(element.textContent).toContain(
      'Nur Studio-Administratoren können die Preisliste ändern.',
    );
    expect(table(element, 'Abzüge')?.textContent).toContain('Matt');
    expect(element.textContent).not.toContain('hinzufügen');
    expect(element.querySelector('button[aria-label$="löschen"]')).toBeNull();
    expect(element.querySelector('table a')).toBeNull();
  });

  it('deletes after confirmation and reloads', async () => {
    const { fixture, element } = await render('STUDIO_ADMIN');
    confirm.mockResolvedValue(true);

    (
      element.querySelector('button[aria-label="10 Downloads löschen"]') as HTMLButtonElement
    ).click();
    await settle(fixture);
    httpTesting
      .expectOne({ method: 'DELETE', url: '/api/studio/price-list/download-packages/d1' })
      .flush(null, { status: 204, statusText: 'No Content' });
    await settle(fixture);
    httpTesting
      .expectOne({ method: 'GET', url: '/api/studio/price-list' })
      .flush({ ...priceList(), downloadPackages: [] });
    await settle(fixture);

    expect(notifications.success).toHaveBeenCalledWith('„10 Downloads“ wurde gelöscht.');
    expect(element.textContent).toContain('Noch keine Pakete');
  });

  it('does not delete without confirmation', async () => {
    const { fixture, element } = await render('STUDIO_ADMIN');
    confirm.mockResolvedValue(false);

    (
      element.querySelector(
        'button[aria-label="Download Volle Auflösung löschen"]',
      ) as HTMLButtonElement
    ).click();
    await settle(fixture);

    httpTesting.expectNone({ method: 'DELETE' });
  });

  it('hides "add download" when every resolution has a price', async () => {
    const list = priceList();
    list.products.push({
      id: 'p4',
      type: 'DOWNLOAD',
      resolution: 'WEB',
      priceCents: 490,
      active: true,
      ...timestamps,
    });
    const { element } = await render('STUDIO_ADMIN', list);

    expect(element.textContent).not.toContain('Download hinzufügen');
  });
});
