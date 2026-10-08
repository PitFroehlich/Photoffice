import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { NotificationService } from '../shared/ui';
import { settle } from '../../testing/settle';
import { iconTesting, studioSessionAs } from '../../testing/test-providers';
import { PriceListSettingsForm } from './price-list-settings-form';

describe('PriceListSettingsForm', () => {
  let httpTesting: HttpTestingController;
  const notifications = { success: vi.fn(), error: vi.fn() };

  async function render() {
    await TestBed.configureTestingModule({
      imports: [PriceListSettingsForm, iconTesting],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        studioSessionAs('STUDIO_ADMIN'),
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(PriceListSettingsForm);
    fixture.detectChanges();
    httpTesting.expectOne({ method: 'GET', url: '/api/studio/price-list' }).flush({
      settings: { currency: 'EUR', vatRatePercent: 19 },
      products: [],
      downloadPackages: [],
      shippingMethods: [],
    });
    await settle(fixture);
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  function fill(element: HTMLElement, value: string) {
    const input = element.querySelector('[formcontrolname="vatRate"]') as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  afterEach(() => httpTesting.verify());

  it('saves a VAT rate with a German decimal comma', async () => {
    const { fixture, element } = await render();
    expect((element.querySelector('[formcontrolname="vatRate"]') as HTMLInputElement).value).toBe(
      '19',
    );

    fill(element, '7,5');
    (element.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await settle(fixture);

    const request = httpTesting.expectOne({
      method: 'PUT',
      url: '/api/studio/price-list/settings',
    });
    expect(request.request.body).toEqual({ vatRatePercent: 7.5 });
    request.flush({ currency: 'EUR', vatRatePercent: 7.5 });
    await settle(fixture);
    expect(notifications.success).toHaveBeenCalledWith('Der Steuersatz wurde gespeichert.');
  });

  it('rejects more than two decimal places', async () => {
    const { fixture, element } = await render();
    fill(element, '19,123');
    (element.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await settle(fixture);

    httpTesting.expectNone({ method: 'PUT' });
    expect(element.querySelector('mat-error')?.textContent).toContain('höchstens zwei Nachkommastellen');
  });
});
