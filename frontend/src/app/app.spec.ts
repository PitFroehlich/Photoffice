import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from './auth/auth.service';

describe('App', () => {
  const loggedIn = signal(false);
  const auth = { isLoggedIn: loggedIn.asReadonly(), login: vi.fn(), logout: vi.fn() };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    }).compileComponents();
  });

  it('offers the studio login when logged out', async () => {
    loggedIn.set(false);
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const button = (fixture.nativeElement as HTMLElement).querySelector('button')!;
    expect(button.textContent).toContain('Studio-Login');
    button.click();
    expect(auth.login).toHaveBeenCalled();
  });

  it('offers logout when logged in', async () => {
    loggedIn.set(true);
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const button = (fixture.nativeElement as HTMLElement).querySelector('button')!;
    expect(button.textContent).toContain('Abmelden');
    button.click();
    expect(auth.logout).toHaveBeenCalled();
  });
});
