import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authProvider, fakeAuthService, iconTesting } from '../../testing/test-providers';
import { PublicShell } from './public-shell';

describe('PublicShell', () => {
  async function render(loggedIn: boolean, roles?: string[]) {
    const auth = fakeAuthService(loggedIn, roles);
    await TestBed.configureTestingModule({
      imports: [PublicShell, iconTesting],
      providers: [provideRouter([]), authProvider(auth)],
    }).compileComponents();
    const fixture = TestBed.createComponent(PublicShell);
    await fixture.whenStable();
    return { fixture, auth, element: fixture.nativeElement as HTMLElement };
  }

  it('offers the studio login when logged out', async () => {
    const { element, auth } = await render(false);

    const button = element.querySelector('mat-toolbar button') as HTMLButtonElement;
    expect(button.textContent).toContain('Studio-Login');
    button.click();
    expect(auth.login).toHaveBeenCalled();
  });

  it('links to the studio area when logged in', async () => {
    const { element } = await render(true);

    expect(element.querySelector('mat-toolbar a[href="/studio"]')?.textContent).toContain(
      'Zum Studio-Bereich',
    );
  });

  it('offers the platform login when logged out', async () => {
    const { element } = await render(false);

    expect(element.querySelector('mat-toolbar a[href="/plattform"]')?.textContent).toContain(
      'Plattform-Login',
    );
  });

  it('links the platform operator to the platform area', async () => {
    const { element } = await render(true, ['platform-admin']);

    expect(element.querySelector('mat-toolbar a[href="/plattform"]')?.textContent).toContain(
      'Zur Plattform-Verwaltung',
    );
    expect(element.querySelector('mat-toolbar a[href="/studio"]')).toBeNull();
  });

  it('has a skip link to the main content', async () => {
    const { element } = await render(false);

    expect(element.querySelector('.skip-link')?.getAttribute('href')).toBe('#main-content');
    expect(element.querySelector('main#main-content')).not.toBeNull();
  });
});
