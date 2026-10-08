import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NotificationService, provideStudioUiDefaults } from '../shared/ui';
import { StudioProfile } from '../api/models';
import { settle } from '../../testing/settle';
import { iconTesting, studioSessionAs } from '../../testing/test-providers';
import { StudioProfileForm } from './studio-profile-form';

const savedProfile: StudioProfile = {
  displayName: 'Studio A Fotografie',
  street: 'Lindenstraße 4',
  postalCode: '10969',
  city: 'Berlin',
  country: 'DE',
  email: 'kontakt@studio-a.example',
  website: 'https://studio-a.example',
  vatId: 'DE123456789',
  accountHolder: 'Studio A Fotografie GmbH',
  iban: 'DE89370400440532013000',
  bic: 'COBADEFFXXX',
  updatedAt: '2026-10-08T10:15:00Z',
};

describe('StudioProfileForm', () => {
  let httpTesting: HttpTestingController;
  const notifications = { success: vi.fn(), error: vi.fn() };

  async function render(
    role: 'STUDIO_ADMIN' | 'PHOTOGRAPHER' = 'STUDIO_ADMIN',
    profile = savedProfile,
  ) {
    notifications.success.mockClear();
    await TestBed.configureTestingModule({
      imports: [StudioProfileForm, iconTesting],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideStudioUiDefaults(),
        studioSessionAs(role),
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(StudioProfileForm);
    fixture.detectChanges();
    httpTesting.expectOne({ method: 'GET', url: '/api/studio/profile' }).flush(profile);
    await settle(fixture);
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  function input(element: HTMLElement, name: string): HTMLInputElement {
    return element.querySelector(`[formcontrolname="${name}"]`) as HTMLInputElement;
  }

  function fill(element: HTMLElement, name: string, value: string) {
    const field = input(element, name);
    field.value = value;
    field.dispatchEvent(new Event('input'));
  }

  function submit(element: HTMLElement) {
    (element.querySelector('button[type="submit"]') as HTMLButtonElement).click();
  }

  afterEach(() => httpTesting.verify());

  it('shows the saved profile', async () => {
    const { element } = await render();
    expect(input(element, 'displayName').value).toBe('Studio A Fotografie');
    expect(input(element, 'iban').value).toBe('DE89370400440532013000');
    expect(element.textContent).toContain('Zuletzt gespeichert: 08.10.2026');
  });

  it('saves trimmed values and shows the normalised result', async () => {
    const { fixture, element } = await render();
    fill(element, 'phone', ' 030 1234567 ');
    fill(element, 'website', '');
    fill(element, 'vatId', 'de 123 456 789');
    submit(element);
    await settle(fixture);

    const request = httpTesting.expectOne({ method: 'PUT', url: '/api/studio/profile' });
    expect(request.request.body).toEqual({
      displayName: 'Studio A Fotografie',
      street: 'Lindenstraße 4',
      postalCode: '10969',
      city: 'Berlin',
      country: 'DE',
      email: 'kontakt@studio-a.example',
      phone: '030 1234567',
      website: undefined,
      taxNumber: undefined,
      vatId: 'de 123 456 789',
      accountHolder: 'Studio A Fotografie GmbH',
      iban: 'DE89370400440532013000',
      bic: 'COBADEFFXXX',
    });
    request.flush({
      ...savedProfile,
      phone: '030 1234567',
      website: undefined,
      vatId: 'DE123456789',
    });
    await settle(fixture);
    expect(notifications.success).toHaveBeenCalledWith('Das Studio-Profil wurde gespeichert.');
    expect(input(element, 'vatId').value).toBe('DE123456789');
  });

  it('shows format errors while typing and does not save', async () => {
    const { fixture, element } = await render();
    fill(element, 'iban', 'DE89 3704 0044 0532 0130 01');
    fill(element, 'vatId', 'DE12345');
    await settle(fixture);
    expect(element.textContent).toContain('Bitte eine gültige IBAN eingeben (Prüfsumme)');
    expect(element.textContent).toContain('z. B. DE123456789, ATU12345678');

    submit(element);
    await settle(fixture);
    httpTesting.expectNone({ method: 'PUT' });
  });

  it('checks the postal code against the country', async () => {
    const { fixture, element } = await render();
    fill(element, 'postalCode', '1060');
    await settle(fixture);
    expect(element.textContent).toContain('5 Ziffern für Deutschland');

    fixture.componentInstance['form'].controls.country.setValue('AT');
    await settle(fixture);
    expect(element.textContent).not.toContain('5 Ziffern für Deutschland');
    expect(fixture.componentInstance['form'].controls.postalCode.valid).toBe(true);
  });

  it('requires account holder and IBAN together', async () => {
    const { fixture, element } = await render('STUDIO_ADMIN', {
      displayName: 'Studio B',
      country: 'AT',
    });
    fill(element, 'accountHolder', 'Studio B e.U.');
    submit(element);
    await settle(fixture);

    httpTesting.expectNone({ method: 'PUT' });
    const form = fixture.componentInstance['form'];
    expect(form.controls.iban.hasError('required')).toBe(true);

    fill(element, 'iban', 'AT61 1904 3002 3457 3201');
    await settle(fixture);
    expect(form.valid).toBe(true);
  });

  it('is read-only for photographers', async () => {
    const { element } = await render('PHOTOGRAPHER');
    expect(element.textContent).toContain(
      'Nur Studio-Administratoren können das Studio-Profil ändern.',
    );
    expect(input(element, 'displayName').disabled).toBe(true);
    expect(element.querySelector('button[type="submit"]')).toBeNull();
  });
});
