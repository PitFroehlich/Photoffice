import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { TenantResponse } from '../api/models';
import { NotificationService, provideStudioUiDefaults } from '../shared/ui';
import { settle } from '../../testing/settle';
import { iconTesting } from '../../testing/test-providers';
import { StudioForm } from './studio-form';
import { studioPatterns, suggestSlug } from './studio-validators';

describe('StudioForm', () => {
  let httpTesting: HttpTestingController;
  const notifications = { success: vi.fn(), error: vi.fn() };

  /** @param id studio to edit; registers a new studio without */
  async function render(id?: string) {
    notifications.success.mockReset();
    notifications.error.mockReset();
    await TestBed.configureTestingModule({
      imports: [StudioForm, iconTesting],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideStudioUiDefaults(),
        { provide: NotificationService, useValue: notifications },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap(id ? { id } : {}) } },
        },
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(StudioForm);
    fixture.detectChanges();
    return { fixture, navigate, element: fixture.nativeElement as HTMLElement };
  }

  function input(element: HTMLElement, name: string) {
    return element.querySelector(`[formcontrolname="${name}"]`) as HTMLInputElement;
  }

  function fill(element: HTMLElement, name: string, value: string) {
    const field = input(element, name);
    field.value = value;
    field.dispatchEvent(new Event('input'));
  }

  const submit = (element: HTMLElement) =>
    (element.querySelector('button[type="submit"]') as HTMLButtonElement).click();

  afterEach(() => httpTesting.verify());

  it('registers a studio with its first admin and suggests the slug from the name', async () => {
    const { fixture, element, navigate } = await render();
    fill(element, 'name', ' Fotostudio Müller ');
    fill(element, 'adminEmail', 'inhaber@studio-mueller.test');
    fill(element, 'adminFirstName', 'Maria');
    fixture.detectChanges();
    expect(input(element, 'slug').value).toBe('fotostudio-mueller');

    submit(element);
    await settle(fixture);
    const request = httpTesting.expectOne({ method: 'POST', url: '/api/platform/tenants' });
    expect(request.request.body).toEqual({
      name: 'Fotostudio Müller',
      slug: 'fotostudio-mueller',
      adminEmail: 'inhaber@studio-mueller.test',
      adminFirstName: 'Maria',
    });
    request.flush({
      id: 'new',
      slug: 'fotostudio-mueller',
      name: 'Fotostudio Müller',
      status: 'ACTIVE',
    });
    await settle(fixture);

    expect(notifications.success).toHaveBeenCalledWith(
      expect.stringContaining('„Fotostudio Müller“ wurde registriert.'),
    );
    expect(navigate).toHaveBeenCalledWith(['/plattform/studios']);
  });

  it('keeps a slug edited by the user', async () => {
    const { fixture, element } = await render();
    fill(element, 'slug', 'mueller');
    fill(element, 'name', 'Fotostudio Müller');
    fixture.detectChanges();

    expect(input(element, 'slug').value).toBe('mueller');
  });

  it('checks the slug format like the backend', async () => {
    const { fixture, element } = await render();
    fill(element, 'name', 'Studio');
    fill(element, 'slug', 'Studio Müller!');
    fill(element, 'adminEmail', 'inhaber@example.test');
    submit(element);
    await settle(fixture);

    httpTesting.expectNone({ method: 'POST' });
    expect(element.querySelector('mat-error')?.textContent).toContain(
      'Kleinbuchstaben, Ziffern und Bindestriche',
    );
  });

  it('requires name, slug and admin e-mail', async () => {
    const { fixture, element } = await render();
    submit(element);
    await settle(fixture);

    httpTesting.expectNone({ method: 'POST' });
    expect(element.querySelectorAll('mat-error').length).toBe(3);
  });

  it('shows a taken slug at the field', async () => {
    const { fixture, element } = await render();
    fill(element, 'name', 'Studio A');
    fill(element, 'adminEmail', 'inhaber@example.test');
    submit(element);
    await settle(fixture);

    httpTesting
      .expectOne({ method: 'POST', url: '/api/platform/tenants' })
      .flush(
        { status: 409, detail: 'Das Kürzel „studio-a“ ist bereits vergeben.' },
        { status: 409, statusText: 'Conflict' },
      );
    await settle(fixture);

    expect(element.querySelector('mat-error')?.textContent).toContain(
      'Das Kürzel „studio-a“ ist bereits vergeben.',
    );
    expect(notifications.error).not.toHaveBeenCalled();
  });
});

describe('StudioForm (edit)', () => {
  let httpTesting: HttpTestingController;
  const notifications = { success: vi.fn(), error: vi.fn() };

  const failedStudio: TenantResponse = {
    id: 'id-k',
    slug: 'konflikt-studio',
    name: 'Konflikt-Studio',
    status: 'ACTIVE',
    onboardingStatus: 'PENDING',
    onboardingError: 'Die E-Mail-Adresse admin@studio-a.test gehört bereits zu Studio „Studio A“.',
    onboardingFailedAt: '2026-10-09T10:00:00Z',
    createdAt: '2026-10-09T09:59:00Z',
    adminEmail: 'admin@studio-a.test',
    adminFirstName: 'Anna',
  };

  async function render(studio: TenantResponse) {
    notifications.success.mockReset();
    notifications.error.mockReset();
    await TestBed.configureTestingModule({
      imports: [StudioForm, iconTesting],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideStudioUiDefaults(),
        { provide: NotificationService, useValue: notifications },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: studio.id }) } },
        },
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(StudioForm);
    fixture.detectChanges();
    httpTesting
      .expectOne({ method: 'GET', url: `/api/platform/tenants/${studio.id}` })
      .flush(studio);
    await settle(fixture);
    return { fixture, navigate, element: fixture.nativeElement as HTMLElement };
  }

  const input = (element: HTMLElement, name: string) =>
    element.querySelector(`[formcontrolname="${name}"]`) as HTMLInputElement | null;

  function fill(element: HTMLElement, name: string, value: string) {
    const field = input(element, name)!;
    field.value = value;
    field.dispatchEvent(new Event('input'));
  }

  const submit = (element: HTMLElement) =>
    (element.querySelector('button[type="submit"]') as HTMLButtonElement).click();

  afterEach(() => httpTesting.verify());

  it('shows the failed onboarding and saves corrected admin data', async () => {
    const { fixture, element, navigate } = await render(failedStudio);

    expect(element.querySelector('h1')?.textContent).toContain('Konflikt-Studio bearbeiten');
    expect(element.querySelector('.onboarding-error')?.textContent).toContain(
      'gehört bereits zu Studio „Studio A“',
    );
    expect(input(element, 'slug')!.disabled).toBe(true);
    expect(input(element, 'adminEmail')!.value).toBe('admin@studio-a.test');
    expect(input(element, 'adminFirstName')!.value).toBe('Anna');

    fill(element, 'name', 'Konflikt-Studio Neu');
    fill(element, 'adminEmail', ' inhaber@konflikt.test ');
    fill(element, 'adminFirstName', '');
    fixture.detectChanges();
    // The slug is not suggested from the name when editing
    expect(input(element, 'slug')!.value).toBe('konflikt-studio');
    submit(element);
    await settle(fixture);

    const request = httpTesting.expectOne({ method: 'PATCH', url: '/api/platform/tenants/id-k' });
    expect(request.request.body).toEqual({
      name: 'Konflikt-Studio Neu',
      adminEmail: 'inhaber@konflikt.test',
    });
    request.flush({ ...failedStudio, name: 'Konflikt-Studio Neu' });
    await settle(fixture);

    expect(notifications.success).toHaveBeenCalledWith(
      '„Konflikt-Studio Neu“ wurde gespeichert. Das Onboarding wird mit den neuen Daten erneut versucht.',
    );
    expect(navigate).toHaveBeenCalledWith(['/plattform/studios']);
  });

  it('only changes the name after onboarding completed', async () => {
    const { fixture, element } = await render({
      ...failedStudio,
      onboardingStatus: 'COMPLETED',
      onboardingError: undefined,
      adminEmail: undefined,
      adminFirstName: undefined,
    });

    expect(element.querySelector('.onboarding-error')).toBeNull();
    expect(input(element, 'adminEmail')).toBeNull();
    expect(element.textContent).toContain('Das Onboarding ist abgeschlossen');

    fill(element, 'name', 'Neuer Name');
    submit(element);
    await settle(fixture);

    const request = httpTesting.expectOne({ method: 'PATCH', url: '/api/platform/tenants/id-k' });
    expect(request.request.body).toEqual({ name: 'Neuer Name' });
    request.flush({ ...failedStudio, name: 'Neuer Name', onboardingStatus: 'COMPLETED' });
    await settle(fixture);
    expect(notifications.success).toHaveBeenCalledWith('„Neuer Name“ wurde gespeichert.');
  });

  it('shows a conflict as notification', async () => {
    const { fixture, element, navigate } = await render(failedStudio);
    submit(element);
    await settle(fixture);

    httpTesting
      .expectOne({ method: 'PATCH', url: '/api/platform/tenants/id-k' })
      .flush(
        { status: 409, detail: 'Das Onboarding von „Konflikt-Studio“ ist bereits abgeschlossen.' },
        { status: 409, statusText: 'Conflict' },
      );
    await settle(fixture);

    expect(notifications.error).toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });

  it('goes back to the list if the studio does not exist', async () => {
    notifications.error.mockReset();
    await TestBed.configureTestingModule({
      imports: [StudioForm, iconTesting],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideStudioUiDefaults(),
        { provide: NotificationService, useValue: notifications },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: 'unknown' }) } },
        },
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(StudioForm);
    fixture.detectChanges();
    httpTesting
      .expectOne({ method: 'GET', url: '/api/platform/tenants/unknown' })
      .flush({ status: 404 }, { status: 404, statusText: 'Not Found' });
    await settle(fixture);

    expect(notifications.error).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/plattform/studios']);
  });
});

describe('studio validators', () => {
  it('suggests slugs that match the backend format', () => {
    expect(suggestSlug('Fotostudio Müller')).toBe('fotostudio-mueller');
    expect(suggestSlug('  Café & Bild – Groß  ')).toBe('cafe-bild-gross');
    expect(suggestSlug('Studio 42')).toMatch(studioPatterns.slug);
  });

  it('accepts slugs like the backend', () => {
    expect(studioPatterns.slug.test('studio-a')).toBe(true);
    expect(studioPatterns.slug.test('ab')).toBe(false);
    expect(studioPatterns.slug.test('-studio')).toBe(false);
    expect(studioPatterns.slug.test('studio-')).toBe(false);
    expect(studioPatterns.slug.test('Studio')).toBe(false);
    expect(studioPatterns.slug.test('a'.repeat(63))).toBe(true);
    expect(studioPatterns.slug.test('a'.repeat(64))).toBe(false);
  });
});
