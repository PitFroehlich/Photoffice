import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { NotificationService } from '../shared/ui';
import { settle } from '../../testing/settle';
import { iconTesting, studioSessionAs } from '../../testing/test-providers';
import { ProductForm } from './product-form';

describe('ProductForm', () => {
  let httpTesting: HttpTestingController;
  const notifications = { success: vi.fn(), error: vi.fn() };

  async function render(
    id: string | null,
    typ?: string,
    role: 'STUDIO_ADMIN' | 'PHOTOGRAPHER' = 'STUDIO_ADMIN',
  ) {
    notifications.success.mockReset();
    await TestBed.configureTestingModule({
      imports: [ProductForm, iconTesting],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        studioSessionAs(role),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap(id ? { id } : {}),
              queryParamMap: convertToParamMap(typ ? { typ } : {}),
            },
          },
        },
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(ProductForm);
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

  it('creates a print with the price in cents', async () => {
    const { fixture, element, navigate } = await render(null);
    expect(element.querySelector('h1')?.textContent).toContain('Neuer Abzug');
    fill(element, 'paperType', ' Matt ');
    fill(element, 'printFormat', '13 × 18 cm');
    fill(element, 'price', '2,9');
    submit(element);
    await settle(fixture);

    const request = httpTesting.expectOne({
      method: 'POST',
      url: '/api/studio/price-list/products',
    });
    expect(request.request.body).toEqual({
      type: 'PRINT',
      paperType: 'Matt',
      printFormat: '13 × 18 cm',
      priceCents: 290,
      active: true,
    });
    request.flush({ id: 'new' });
    await settle(fixture);

    expect(notifications.success).toHaveBeenCalledWith('„Abzug Matt, 13 × 18 cm“ wurde angelegt.');
    expect(navigate).toHaveBeenCalledWith(['/studio/preisliste']);
  });

  it('creates a download variant with name and size', async () => {
    const { fixture, element } = await render(null, 'download');
    expect(element.querySelector('[formcontrolname="paperType"]')).toBeNull();
    fill(element, 'downloadName', ' Web 2048 px ');
    fill(element, 'maxEdgePx', '2048');
    fill(element, 'price', '4,90');
    submit(element);
    await settle(fixture);

    const request = httpTesting.expectOne({
      method: 'POST',
      url: '/api/studio/price-list/products',
    });
    expect(request.request.body).toEqual({
      type: 'DOWNLOAD',
      downloadName: 'Web 2048 px',
      maxEdgePx: 2048,
      priceCents: 490,
      active: true,
    });
    request.flush({ id: 'new' });
    await settle(fixture);
    expect(notifications.success).toHaveBeenCalledWith('„Download Web 2048 px“ wurde angelegt.');
  });

  it('sends no size for the original', async () => {
    const { fixture, element } = await render(null, 'download');
    fill(element, 'downloadName', 'Original');
    fill(element, 'price', '9,90');
    submit(element);
    await settle(fixture);

    const request = httpTesting.expectOne({
      method: 'POST',
      url: '/api/studio/price-list/products',
    });
    expect(request.request.body.maxEdgePx).toBeUndefined();
    request.flush({ id: 'new' });
    await settle(fixture);
  });

  it('rejects a size outside 200 to 20,000 pixels', async () => {
    const { fixture, element } = await render(null, 'download');
    fill(element, 'downloadName', 'Mini');
    fill(element, 'maxEdgePx', '100');
    fill(element, 'price', '1');
    submit(element);
    await settle(fixture);

    httpTesting.expectNone({ method: 'POST' });
  });

  it('does not send an invalid price', async () => {
    const { fixture, element } = await render(null);
    fill(element, 'paperType', 'Matt');
    fill(element, 'printFormat', '10 × 15 cm');
    fill(element, 'price', '1,999');
    submit(element);
    await settle(fixture);

    httpTesting.expectNone({ method: 'POST' });
    expect(element.querySelector('mat-error')?.textContent).toContain('z. B. 12,90');
  });

  it('shows an existing combination at the format field', async () => {
    const { fixture, element } = await render(null);
    fill(element, 'paperType', 'Matt');
    fill(element, 'printFormat', '13 × 18 cm');
    fill(element, 'price', '2,90');
    submit(element);
    await settle(fixture);

    httpTesting
      .expectOne({ method: 'POST', url: '/api/studio/price-list/products' })
      .flush(
        { status: 409, detail: 'Den Abzug „Matt, 13 × 18 cm“ gibt es bereits.' },
        { status: 409, statusText: 'Conflict' },
      );
    await settle(fixture);

    expect(element.querySelector('mat-error')?.textContent).toContain('gibt es bereits');
    expect(notifications.success).not.toHaveBeenCalled();
  });

  it('loads and updates an existing product', async () => {
    const { fixture, element } = await render('p1');
    httpTesting.expectOne({ method: 'GET', url: '/api/studio/price-list/products/p1' }).flush({
      id: 'p1',
      type: 'PRINT',
      paperType: 'Matt',
      printFormat: '13 × 18 cm',
      priceCents: 290,
      active: true,
    });
    await settle(fixture);

    expect(element.querySelector('h1')?.textContent).toContain('Abzug Matt, 13 × 18 cm');
    expect((element.querySelector('[formcontrolname="price"]') as HTMLInputElement).value).toBe(
      '2,90',
    );

    fill(element, 'price', '3,10');
    submit(element);
    await settle(fixture);

    const request = httpTesting.expectOne({
      method: 'PUT',
      url: '/api/studio/price-list/products/p1',
    });
    expect(request.request.body.priceCents).toBe(310);
    request.flush({ id: 'p1' });
    await settle(fixture);
    expect(notifications.success).toHaveBeenCalledWith(
      '„Abzug Matt, 13 × 18 cm“ wurde gespeichert.',
    );
  });

  it('does not let photographers save', async () => {
    const { element } = await render(null, undefined, 'PHOTOGRAPHER');

    expect(element.textContent).toContain('Nur Studio-Administratoren');
    expect((element.querySelector('button[type="submit"]') as HTMLButtonElement).disabled).toBe(
      true,
    );
  });
});
