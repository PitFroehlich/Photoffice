import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { settle } from '../../testing/settle';
import { StartPage } from './start-page';

describe('StartPage', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StartPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
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
});
