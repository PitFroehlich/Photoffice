import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { NotificationService } from '../shared/ui';
import { settle } from '../../testing/settle';
import { iconTesting, studioSessionAs } from '../../testing/test-providers';
import { DownloadPackageForm } from './download-package-form';

describe('DownloadPackageForm', () => {
  let httpTesting: HttpTestingController;
  const notifications = { success: vi.fn(), error: vi.fn() };

  async function render(id: string | null) {
    notifications.success.mockReset();
    await TestBed.configureTestingModule({
      imports: [DownloadPackageForm, iconTesting],
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
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(DownloadPackageForm);
    fixture.detectChanges();
    // The form first loads the download variants of the studio
    httpTesting.expectOne({ method: 'GET', url: '/api/studio/price-list' }).flush({
      settings: { currency: 'EUR', vatRatePercent: 19 },
      products: variants,
      downloadPackages: [],
      shippingMethods: [],
    });
    await settle(fixture);
    const form = (fixture.componentInstance as unknown as { form: DownloadPackageForm['form'] })
      .form;
    return { fixture, form, element: fixture.nativeElement as HTMLElement };
  }

  const defaultVariants = [
    {
      id: 'v-web',
      type: 'DOWNLOAD',
      downloadName: 'Web 2048 px',
      maxEdgePx: 2048,
      priceCents: 490,
      active: true,
    },
    { id: 'v-original', type: 'DOWNLOAD', downloadName: 'Original', priceCents: 990, active: true },
  ];
  let variants: unknown[] = defaultVariants;
  beforeEach(() => (variants = defaultVariants));

  function fill(element: HTMLElement, name: string, value: string) {
    const input = element.querySelector(`[formcontrolname="${name}"]`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  const submit = (element: HTMLElement) =>
    (element.querySelector('button[type="submit"]') as HTMLButtonElement).click();

  afterEach(() => httpTesting.verify());

  it('creates a package with a fixed number of images', async () => {
    const { fixture, element } = await render(null);
    fill(element, 'name', '10 Downloads');
    fill(element, 'price', '69');
    submit(element);
    await settle(fixture);

    const request = httpTesting.expectOne({
      method: 'POST',
      url: '/api/studio/price-list/download-packages',
    });
    expect(request.request.body).toEqual({
      name: '10 Downloads',
      kind: 'IMAGE_COUNT',
      imageCount: 10,
      downloadProductId: 'v-original',
      priceCents: 6900,
      active: true,
    });
    request.flush({ id: 'new' });
    await settle(fixture);
    expect(notifications.success).toHaveBeenCalledWith('„10 Downloads“ wurde angelegt.');
  });

  it('has no image count for the whole gallery', async () => {
    const { fixture, form, element } = await render(null);
    form.controls.kind.setValue('WHOLE_GALLERY');
    await settle(fixture);
    expect(element.querySelector('[formcontrolname="imageCount"]')).toBeNull();

    fill(element, 'name', 'Ganze Galerie');
    fill(element, 'price', '149,00');
    submit(element);
    await settle(fixture);

    const request = httpTesting.expectOne({
      method: 'POST',
      url: '/api/studio/price-list/download-packages',
    });
    expect(request.request.body.kind).toBe('WHOLE_GALLERY');
    expect(request.request.body.imageCount).toBeUndefined();
    request.flush({ id: 'new' });
    await settle(fixture);
  });

  it('shows a duplicate name at the field', async () => {
    const { fixture, element } = await render(null);
    fill(element, 'name', 'Ganze Galerie');
    fill(element, 'price', '149');
    submit(element);
    await settle(fixture);

    httpTesting
      .expectOne({ method: 'POST', url: '/api/studio/price-list/download-packages' })
      .flush(
        { status: 409, detail: 'Ein Paket mit dem Namen „Ganze Galerie“ gibt es bereits.' },
        { status: 409, statusText: 'Conflict' },
      );
    await settle(fixture);

    expect(element.querySelector('mat-error')?.textContent).toContain('gibt es bereits');
  });

  it('loads an existing package', async () => {
    const { fixture, element } = await render('d1');
    httpTesting
      .expectOne({ method: 'GET', url: '/api/studio/price-list/download-packages/d1' })
      .flush({
        id: 'd1',
        name: 'Ganze Galerie',
        kind: 'WHOLE_GALLERY',
        downloadProductId: 'v-web',
        priceCents: 14900,
        active: false,
      });
    await settle(fixture);

    expect(element.querySelector('h1')?.textContent).toContain('Ganze Galerie');
    expect((element.querySelector('[formcontrolname="price"]') as HTMLInputElement).value).toBe(
      '149,00',
    );
    expect(element.querySelector('[formcontrolname="imageCount"]')).toBeNull();
  });

  it('asks for a download variant first when there is none', async () => {
    variants = [];
    const { element } = await render(null);

    expect(element.textContent).toContain(
      'Legen Sie zuerst unter „Downloads“ eine Download-Variante an',
    );
    expect((element.querySelector('button[type="submit"]') as HTMLButtonElement).disabled).toBe(
      true,
    );
  });
});
