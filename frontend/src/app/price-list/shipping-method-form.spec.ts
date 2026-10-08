import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { NotificationService } from '../shared/ui';
import { settle } from '../../testing/settle';
import { iconTesting, studioSessionAs } from '../../testing/test-providers';
import { ShippingMethodForm } from './shipping-method-form';

describe('ShippingMethodForm', () => {
  let httpTesting: HttpTestingController;
  const notifications = { success: vi.fn(), error: vi.fn() };

  async function render(id: string | null) {
    notifications.success.mockReset();
    await TestBed.configureTestingModule({
      imports: [ShippingMethodForm, iconTesting],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        studioSessionAs('STUDIO_ADMIN'),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap(id ? { id } : {}) } },
        },
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(ShippingMethodForm);
    fixture.detectChanges();
    return { fixture, navigate, element: fixture.nativeElement as HTMLElement };
  }

  function fill(element: HTMLElement, name: string, value: string) {
    const input = element.querySelector(`[formcontrolname="${name}"]`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  const submit = (element: HTMLElement) =>
    (element.querySelector('button[type="submit"]') as HTMLButtonElement).click();

  afterEach(() => httpTesting.verify());

  it('creates a free shipping method', async () => {
    const { fixture, element, navigate } = await render(null);
    fill(element, 'name', 'Abholung im Studio');
    fill(element, 'price', '0');
    submit(element);
    await settle(fixture);

    const request = httpTesting.expectOne({
      method: 'POST',
      url: '/api/studio/price-list/shipping-methods',
    });
    expect(request.request.body).toEqual({
      name: 'Abholung im Studio',
      priceCents: 0,
      active: true,
    });
    request.flush({ id: 'new' });
    await settle(fixture);

    expect(notifications.success).toHaveBeenCalledWith('„Abholung im Studio“ wurde angelegt.');
    expect(navigate).toHaveBeenCalledWith(['/studio/preisliste']);
  });

  it('requires a price', async () => {
    const { fixture, element } = await render(null);
    fill(element, 'name', 'Standardversand');
    submit(element);
    await settle(fixture);

    httpTesting.expectNone({ method: 'POST' });
    expect(element.querySelector('mat-error')?.textContent).toContain('Pflichtfeld');
  });

  it('loads and deactivates an existing shipping method', async () => {
    const { fixture, element } = await render('s1');
    httpTesting
      .expectOne({ method: 'GET', url: '/api/studio/price-list/shipping-methods/s1' })
      .flush({ id: 's1', name: 'Expressversand', priceCents: 1290, active: true });
    await settle(fixture);
    expect((element.querySelector('[formcontrolname="price"]') as HTMLInputElement).value).toBe(
      '12,90',
    );

    (element.querySelector('mat-slide-toggle button') as HTMLButtonElement).click();
    submit(element);
    await settle(fixture);

    const request = httpTesting.expectOne({
      method: 'PUT',
      url: '/api/studio/price-list/shipping-methods/s1',
    });
    expect(request.request.body).toEqual({
      name: 'Expressversand',
      priceCents: 1290,
      active: false,
    });
    request.flush({ id: 's1' });
    await settle(fixture);
  });
});
