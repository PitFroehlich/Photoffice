import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authProvider, fakeAuthService, iconTesting } from '../../testing/test-providers';
import { PlatformShell } from './platform-shell';

describe('PlatformShell', () => {
  async function render() {
    const auth = fakeAuthService(true, ['platform-admin']);
    await TestBed.configureTestingModule({
      imports: [PlatformShell, iconTesting],
      providers: [provideRouter([]), authProvider(auth)],
    }).compileComponents();
    const fixture = TestBed.createComponent(PlatformShell);
    await fixture.whenStable();
    return { fixture, auth, element: fixture.nativeElement as HTMLElement };
  }

  it('shows the platform area, the user and the navigation', async () => {
    const { element } = await render();

    expect(element.querySelector('.area-name')?.textContent).toContain('Plattform-Verwaltung');
    expect(element.querySelector('.user-button')?.textContent).toContain('Paula Plattform');
    expect(element.querySelector('nav[aria-label="Plattform-Navigation"]')?.textContent).toContain(
      'Studios',
    );
    expect(element.querySelector('main#main-content')).not.toBeNull();
  });

  it('logs out from the user menu', async () => {
    const { fixture, auth, element } = await render();

    (element.querySelector('.user-button') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(document.querySelector('.menu-detail')?.textContent).toContain('Plattform-Betreiber');
    const logout = Array.from(document.querySelectorAll('[mat-menu-item]')).find((item) =>
      item.textContent?.includes('Abmelden'),
    ) as HTMLButtonElement;
    logout.click();

    expect(auth.logout).toHaveBeenCalled();
  });
});
