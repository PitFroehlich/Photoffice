import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { ConfirmService, NotificationService, provideStudioUiDefaults } from '../shared/ui';
import { settle } from '../../testing/settle';
import { iconTesting } from '../../testing/test-providers';
import { CustomerForm } from './customer-form';

describe('CustomerForm', () => {
  let httpTesting: HttpTestingController;
  const notifications = { success: vi.fn(), error: vi.fn() };

  async function render(id: string | null) {
    notifications.success.mockReset();
    await TestBed.configureTestingModule({
      imports: [CustomerForm, iconTesting],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(id ? { id } : {}) } } },
        { provide: ConfirmService, useValue: { confirm: vi.fn().mockResolvedValue(true) } },
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(CustomerForm);
    fixture.detectChanges();
    return { fixture, navigate, element: fixture.nativeElement as HTMLElement };
  }

  function fill(element: HTMLElement, name: string, value: string) {
    const input = element.querySelector(`[formcontrolname="${name}"]`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  afterEach(() => httpTesting.verify());

  it('creates a customer with trimmed values', async () => {
    const { fixture, element, navigate } = await render(null);
    fill(element, 'firstName', ' Julia ');
    fill(element, 'lastName', 'Becker');
    fill(element, 'email', 'julia@example.test');
    (element.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await settle(fixture);

    const request = httpTesting.expectOne({ method: 'POST', url: '/api/studio/customers' });
    expect(request.request.body).toEqual({ firstName: 'Julia', lastName: 'Becker', email: 'julia@example.test' });
    request.flush({ id: 'new', firstName: 'Julia', lastName: 'Becker', email: 'julia@example.test' });
    await settle(fixture);

    expect(notifications.success).toHaveBeenCalledWith('„Julia Becker“ wurde angelegt.');
    expect(navigate).toHaveBeenCalledWith(['/studio/kunden']);
  });

  it('does not send an invalid form', async () => {
    const { fixture, element } = await render(null);
    (element.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await settle(fixture);

    httpTesting.expectNone({ method: 'POST' });
    expect(element.querySelector('mat-error')?.textContent).toContain('Pflichtfeld');
  });

  it('shows a duplicate e-mail at the field', async () => {
    const { fixture, element } = await render(null);
    fill(element, 'firstName', 'Julia');
    fill(element, 'lastName', 'Becker');
    fill(element, 'email', 'julia@example.test');
    (element.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await settle(fixture);

    httpTesting.expectOne({ method: 'POST', url: '/api/studio/customers' }).flush(
      { status: 409, detail: 'Ein Kunde mit der E-Mail-Adresse julia@example.test existiert bereits.' },
      { status: 409, statusText: 'Conflict' },
    );
    await settle(fixture);

    expect(element.querySelector('mat-error')?.textContent).toContain('existiert bereits');
    expect(notifications.success).not.toHaveBeenCalled();
  });

  it('loads and updates an existing customer', async () => {
    const { fixture, element } = await render('c1');
    httpTesting.expectOne({ method: 'GET', url: '/api/studio/customers/c1' }).flush({
      id: 'c1',
      firstName: 'Julia',
      lastName: 'Becker',
      email: 'julia@example.test',
      city: 'Berlin',
    });
    await settle(fixture);

    expect(element.querySelector('h1')?.textContent).toContain('Julia Becker');
    expect((element.querySelector('[formcontrolname="city"]') as HTMLInputElement).value).toBe('Berlin');

    fill(element, 'city', 'Potsdam');
    (element.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await settle(fixture);

    const request = httpTesting.expectOne({ method: 'PUT', url: '/api/studio/customers/c1' });
    expect(request.request.body.city).toBe('Potsdam');
    request.flush({ id: 'c1' });
    await settle(fixture);
    expect(notifications.success).toHaveBeenCalledWith('„Julia Becker“ wurde gespeichert.');
  });
});

describe('CustomerForm validation rules', () => {
  async function renderNew() {
    await TestBed.configureTestingModule({
      imports: [CustomerForm, iconTesting],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({}) } } },
        { provide: ConfirmService, useValue: { confirm: vi.fn() } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
        provideStudioUiDefaults(),
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(CustomerForm);
    fixture.detectChanges();
    return fixture;
  }

  async function errorFor(fixture: Awaited<ReturnType<typeof renderNew>>, name: string, value: string) {
    const element = fixture.nativeElement as HTMLElement;
    const input = element.querySelector(`[formcontrolname="${name}"]`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    return input.closest('mat-form-field')?.querySelector('mat-error')?.textContent?.trim() ?? null;
  }

  it('shows field-specific hints while typing', async () => {
    const fixture = await renderNew();

    expect(await errorFor(fixture, 'phone', 'abc')).toContain('Nur Ziffern');
    expect(await errorFor(fixture, 'postalCode', '12a')).toBe('4 oder 5 Ziffern');
    expect(await errorFor(fixture, 'firstName', 'Julia2')).toContain('Nur Buchstaben');
    expect(await errorFor(fixture, 'email', 'julia@example')).toContain('gültige E-Mail-Adresse');
    expect(await errorFor(fixture, 'city', '10115 Berlin')).toContain('Nur Buchstaben');
    expect(await errorFor(fixture, 'street', '12345')).toContain('Straße und Hausnummer');
  });

  it('accepts realistic values, also with surrounding spaces', async () => {
    const fixture = await renderNew();

    expect(await errorFor(fixture, 'phone', '+49 (30) 123-456')).toBeNull();
    expect(await errorFor(fixture, 'postalCode', '10969')).toBeNull();
    expect(await errorFor(fixture, 'firstName', ' Zoë ')).toBeNull();
    expect(await errorFor(fixture, 'city', 'Frankfurt (Oder)')).toBeNull();
    expect(await errorFor(fixture, 'street', 'Lindenstraße 4a')).toBeNull();
  });
});
