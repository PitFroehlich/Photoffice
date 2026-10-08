import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { settle } from '../../testing/settle';
import { authProvider, fakeAuthService, iconTesting } from '../../testing/test-providers';
import { StartPage } from './start-page';

describe('StartPage', () => {
  let httpTesting: HttpTestingController;
  let auth: ReturnType<typeof fakeAuthService>;

  beforeEach(async () => {
    auth = fakeAuthService(false);
    await TestBed.configureTestingModule({
      imports: [StartPage, iconTesting],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), authProvider(auth)],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('shows the backend version from the generated API client', async () => {
    const fixture = TestBed.createComponent(StartPage);
    fixture.detectChanges();

    httpTesting.expectOne('/api/system/info').flush({ name: 'photoffice-backend', version: '1.2.3' });
    await settle(fixture);

    const text = (fixture.nativeElement as HTMLElement).querySelector('.system-info')?.textContent;
    expect(text).toContain('photoffice-backend 1.2.3');
  });

  it('shows a hint when the backend is not reachable', async () => {
    const fixture = TestBed.createComponent(StartPage);
    fixture.detectChanges();

    httpTesting.expectOne('/api/system/info').flush(null, { status: 503, statusText: 'Service Unavailable' });
    await settle(fixture);

    const text = (fixture.nativeElement as HTMLElement).querySelector('.system-info')?.textContent;
    expect(text).toContain('Backend nicht erreichbar');
  });

  it('offers the studio login when logged out', async () => {
    const fixture = TestBed.createComponent(StartPage);
    fixture.detectChanges();
    httpTesting.expectOne('/api/system/info').flush({ name: 'x', version: '1' });
    await settle(fixture);

    const button = (fixture.nativeElement as HTMLElement).querySelector('.actions button') as HTMLButtonElement;
    expect(button.textContent).toContain('Als Studio anmelden');
    button.click();
    expect(auth.login).toHaveBeenCalled();
  });
});
