import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { StudioSession } from './studio-session';

describe('StudioSession', () => {
  let session: StudioSession;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    session = TestBed.inject(StudioSession);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('loads the current user once', async () => {
    const loading = session.load();
    void session.load();
    httpTesting.expectOne('/api/studio/me').flush({
      username: 'admin-a',
      roles: ['STUDIO_ADMIN'],
      studio: { id: '1', slug: 'studio-a', name: 'Studio A' },
    });
    await loading;

    expect(session.status()).toBe('ready');
    expect(session.user()?.studio.name).toBe('Studio A');
  });

  it('marks access as denied when the backend refuses', async () => {
    const loading = session.load();
    httpTesting.expectOne('/api/studio/me').flush(null, { status: 403, statusText: 'Forbidden' });
    await loading;

    expect(session.status()).toBe('denied');
    expect(session.user()).toBeUndefined();
  });
});
