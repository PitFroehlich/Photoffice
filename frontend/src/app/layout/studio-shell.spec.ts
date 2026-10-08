import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { CurrentStudioUser } from '../api/models';
import { StudioSession } from '../studio/studio-session';
import { authProvider, fakeAuthService, iconTesting } from '../../testing/test-providers';
import { StudioShell } from './studio-shell';

const annaAdmin: CurrentStudioUser = {
  username: 'admin-a',
  displayName: 'Anna Admin',
  roles: ['STUDIO_ADMIN'],
  studio: { id: 'a0000000-0000-4000-8000-00000000000a', slug: 'studio-a', name: 'Studio A' },
};

describe('StudioShell', () => {
  function fakeSession(status: 'ready' | 'denied', user?: CurrentStudioUser) {
    return {
      user: signal(user).asReadonly(),
      status: signal(status).asReadonly(),
      load: vi.fn().mockResolvedValue(undefined),
      clear: vi.fn(),
    };
  }

  async function render(session: ReturnType<typeof fakeSession>) {
    const auth = fakeAuthService(true);
    await TestBed.configureTestingModule({
      imports: [StudioShell, iconTesting],
      providers: [
        provideRouter([{ path: '**', children: [] }]),
        authProvider(auth),
        { provide: StudioSession, useValue: session },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(StudioShell);
    await fixture.whenStable();
    return { fixture, auth, element: fixture.nativeElement as HTMLElement };
  }

  it('shows studio, user and navigation', async () => {
    const session = fakeSession('ready', annaAdmin);
    const { element } = await render(session);

    expect(session.load).toHaveBeenCalled();
    expect(element.querySelector('.studio-name')?.textContent).toContain('Studio A');
    expect(element.querySelector('.user-button')?.textContent).toContain('Anna Admin');
    expect(element.querySelector('nav')?.textContent).toContain('Übersicht');
  });

  it('logs out from the user menu', async () => {
    const session = fakeSession('ready', annaAdmin);
    const { fixture, auth, element } = await render(session);

    (element.querySelector('.user-button') as HTMLButtonElement).click();
    await fixture.whenStable();
    const logout = Array.from(document.querySelectorAll('[mat-menu-item]')).find((item) =>
      item.textContent?.includes('Abmelden'),
    ) as HTMLButtonElement;
    expect(document.querySelector('.menu-detail')?.textContent).toContain(
      'Studio-Administrator · Studio A',
    );
    logout.click();

    expect(session.clear).toHaveBeenCalled();
    expect(auth.logout).toHaveBeenCalled();
  });

  it('offers profile and password changes in the user menu', async () => {
    const { fixture, auth, element } = await render(fakeSession('ready', annaAdmin));
    await TestBed.inject(Router).navigateByUrl('/studio/kunden');

    for (const [label, action] of [
      ['Profil bearbeiten', 'UPDATE_PROFILE'],
      ['Passwort ändern', 'UPDATE_PASSWORD'],
    ]) {
      (element.querySelector('.user-button') as HTMLButtonElement).click();
      await fixture.whenStable();
      const item = Array.from(document.querySelectorAll('[mat-menu-item]')).find((menuItem) =>
        menuItem.textContent?.includes(label),
      ) as HTMLButtonElement;
      item.click();
      expect(auth.startAccountAction).toHaveBeenCalledWith(action, '/studio/kunden');
    }
  });

  it('explains when the user has no active studio', async () => {
    const { element } = await render(fakeSession('denied'));

    expect(element.querySelector('main h1')?.textContent).toContain('Kein Zugriff');
  });
});
