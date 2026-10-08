import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { settle } from '../../testing/settle';
import { StudioHome } from './studio-home';

describe('StudioHome', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StudioHome],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('shows the studio and the user of the login', async () => {
    const fixture = TestBed.createComponent(StudioHome);
    fixture.detectChanges();

    httpTesting.expectOne('/api/studio/me').flush({
      username: 'admin-a',
      displayName: 'Anna Admin',
      roles: ['STUDIO_ADMIN'],
      studio: { id: 'a0000000-0000-4000-8000-00000000000a', slug: 'studio-a', name: 'Studio A' },
    });
    await settle(fixture);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent).toContain('Studio A');
    expect(element.querySelector('.welcome')?.textContent).toContain('Anna Admin');
    expect(element.querySelector('.welcome')?.textContent).toContain('Studio-Administrator');
  });

  it('explains when the user has no active studio', async () => {
    const fixture = TestBed.createComponent(StudioHome);
    fixture.detectChanges();

    httpTesting.expectOne('/api/studio/me').flush(null, { status: 403, statusText: 'Forbidden' });
    await settle(fixture);

    expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent).toContain('Kein Zugriff');
  });
});
