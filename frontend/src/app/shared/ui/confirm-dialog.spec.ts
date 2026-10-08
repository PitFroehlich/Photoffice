import { TestBed } from '@angular/core/testing';
import { ConfirmService } from './confirm-dialog';

describe('ConfirmService', () => {
  async function openAndClick(buttonText: string): Promise<boolean> {
    const service = TestBed.inject(ConfirmService);
    const result = service.confirm({ title: 'Löschen?', message: 'Wirklich?', confirmLabel: 'Löschen', destructive: true });
    await new Promise((resolve) => setTimeout(resolve));
    const button = Array.from(document.querySelectorAll('mat-dialog-actions button')).find((b) =>
      b.textContent?.includes(buttonText),
    ) as HTMLButtonElement;
    button.click();
    return result;
  }

  it('resolves to true when confirmed', async () => {
    expect(await openAndClick('Löschen')).toBe(true);
  });

  it('resolves to false when cancelled', async () => {
    expect(await openAndClick('Abbrechen')).toBe(false);
  });
});
