import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Gallery } from '../api/models';
import { ConfirmService, NotificationService, provideStudioUiDefaults } from '../shared/ui';
import { settle } from '../../testing/settle';
import { iconTesting } from '../../testing/test-providers';
import { GalleryForm } from './gallery-form';

const draft = (): Gallery => ({
  id: 'g1',
  name: 'Hochzeit Becker',
  status: 'DRAFT',
  expired: false,
  expiresOn: '2026-12-31',
  customers: [{ id: 'c1', firstName: 'Julia', lastName: 'Becker', email: 'julia@example.test' }],
  createdAt: '2026-10-08T10:00:00Z',
  updatedAt: '2026-10-08T10:00:00Z',
});

describe('GalleryForm', () => {
  let httpTesting: HttpTestingController;
  const notifications = { success: vi.fn(), error: vi.fn() };

  async function render(id: string | null) {
    notifications.success.mockReset();
    notifications.error.mockReset();
    await TestBed.configureTestingModule({
      imports: [GalleryForm, iconTesting],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideStudioUiDefaults(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap(id ? { id } : {}) } },
        },
        { provide: ConfirmService, useValue: { confirm: vi.fn().mockResolvedValue(true) } },
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(GalleryForm);
    fixture.detectChanges();
    return { fixture, navigate, element: fixture.nativeElement as HTMLElement };
  }

  function fill(element: HTMLElement, name: string, value: string) {
    const input = element.querySelector(`[formcontrolname="${name}"]`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  const button = (element: HTMLElement, text: string) =>
    Array.from(element.querySelectorAll('button')).find((b) =>
      b.textContent?.includes(text),
    ) as HTMLButtonElement;

  afterEach(() => httpTesting.verify());

  it('creates a gallery with a German date and stays on it', async () => {
    const { fixture, element, navigate } = await render(null);
    fill(element, 'name', ' Hochzeit Becker ');
    fill(element, 'expiresOn', '31.12.2030');
    (element.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await settle(fixture);

    const request = httpTesting.expectOne({ method: 'POST', url: '/api/studio/galleries' });
    expect(request.request.body).toEqual({
      name: 'Hochzeit Becker',
      expiresOn: '2030-12-31',
      customerIds: [],
    });
    request.flush({ ...draft(), expiresOn: '2030-12-31', customers: [] });
    await settle(fixture);

    expect(notifications.success).toHaveBeenCalledWith('„Hochzeit Becker“ wurde angelegt.');
    expect(navigate).toHaveBeenCalledWith(['/studio/galerien', 'g1'], { replaceUrl: true });
    expect(element.querySelector('[data-testid="gallery-state"]')?.textContent).toContain(
      'Entwurf',
    );
  });

  it('checks the name format while typing', async () => {
    const { fixture, element } = await render(null);
    fill(element, 'name', '!!!');
    await fixture.whenStable();

    const field = element.querySelector('[formcontrolname="name"]')!.closest('mat-form-field')!;
    expect(field.querySelector('mat-error')?.textContent).toContain('Mindestens ein Buchstabe');
  });

  it('explains invalid dates when leaving the date field', async () => {
    const { fixture, element } = await render(null);
    fill(element, 'expiresOn', '31.02.2030');
    element.querySelector('[formcontrolname="expiresOn"]')!.dispatchEvent(new Event('blur'));
    await settle(fixture);

    expect(element.querySelector('mat-error')?.textContent).toContain('TT.MM.JJJJ');
  });

  it('suggests customers and assigns them as chips', async () => {
    const { fixture, element } = await render('g1');
    httpTesting.expectOne({ method: 'GET', url: '/api/studio/galleries/g1' }).flush(draft());
    await settle(fixture);
    expect(element.querySelector('mat-chip-row')?.textContent).toContain('Julia Becker');

    const search = element.querySelector('mat-chip-grid input') as HTMLInputElement;
    search.value = 'neu';
    search.dispatchEvent(new Event('input'));
    await new Promise((resolve) => setTimeout(resolve, 300));
    const request = httpTesting.expectOne((r) => r.url === '/api/studio/customers');
    expect(request.request.params.get('search')).toBe('neu');
    request.flush({
      items: [
        { id: 'c1', firstName: 'Julia', lastName: 'Becker', email: 'julia@example.test' },
        { id: 'c2', firstName: 'Thomas', lastName: 'Neumann', email: 'thomas@example.test' },
      ],
      page: 0,
      size: 10,
      totalElements: 2,
    });
    await settle(fixture);

    // Already assigned customers are not suggested again
    const component = fixture.componentInstance as unknown as {
      suggestions: () => { id: string }[];
    };
    expect(component.suggestions().map((c) => c.id)).toEqual(['c2']);
  });

  it('publishes and shows the backend reason if that is not possible', async () => {
    const { fixture, element } = await render('g1');
    httpTesting.expectOne({ method: 'GET', url: '/api/studio/galleries/g1' }).flush(draft());
    await settle(fixture);

    button(element, 'Veröffentlichen').click();
    await settle(fixture);
    httpTesting
      .expectOne({ method: 'POST', url: '/api/studio/galleries/g1/publish' })
      .flush(
        { status: 409, detail: 'Ohne aktive Preise kann die Galerie nicht veröffentlicht werden.' },
        { status: 409, statusText: 'Conflict' },
      );
    await settle(fixture);
    expect(notifications.error).toHaveBeenCalled();

    button(element, 'Veröffentlichen').click();
    await settle(fixture);
    httpTesting
      .expectOne({ method: 'POST', url: '/api/studio/galleries/g1/publish' })
      .flush({ ...draft(), status: 'ONLINE' });
    await settle(fixture);
    expect(notifications.success).toHaveBeenCalledWith('„Hochzeit Becker“ ist jetzt online.');
    expect(element.querySelector('[data-testid="gallery-state"]')?.textContent).toContain('Online');
    expect(button(element, 'Offline nehmen')).toBeTruthy();
  });
});
