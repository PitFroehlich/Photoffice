import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { StudioSession } from './studio-session';
import { iconTesting } from '../../testing/test-providers';
import { StudioHome } from './studio-home';

describe('StudioHome', () => {
  it('greets the user and shows studio and role', async () => {
    const session = {
      user: signal({
        username: 'foto-a',
        displayName: 'Felix Foto',
        roles: ['PHOTOGRAPHER'],
        studio: { id: 'a0000000-0000-4000-8000-00000000000a', slug: 'studio-a', name: 'Studio A' },
      }).asReadonly(),
    };
    await TestBed.configureTestingModule({
      imports: [StudioHome, iconTesting],
      providers: [{ provide: StudioSession, useValue: session }],
    }).compileComponents();

    const fixture = TestBed.createComponent(StudioHome);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Übersicht');
    expect(element.querySelector('.welcome')?.textContent).toContain('Willkommen, Felix Foto');
    expect(element.querySelector('mat-chip')?.textContent).toContain('Fotograf');
  });
});
