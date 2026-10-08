import { TestBed } from '@angular/core/testing';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { GermanPaginatorIntl } from '../../shared/ui/german-paginator-intl';
import { ConfirmService, NotificationService } from '../../shared/ui';
import { iconTesting } from '../../../testing/test-providers';
import { UiShowcase } from './ui-showcase';

describe('UiShowcase', () => {
  const confirm = vi.fn();
  const notifications = { success: vi.fn(), error: vi.fn() };

  beforeEach(async () => {
    confirm.mockReset();
    notifications.success.mockReset();
    await TestBed.configureTestingModule({
      imports: [UiShowcase, iconTesting],
      providers: [
        { provide: ConfirmService, useValue: { confirm } },
        { provide: NotificationService, useValue: notifications },
        { provide: MatPaginatorIntl, useClass: GermanPaginatorIntl },
      ],
    }).compileComponents();
  });

  function render() {
    const fixture = TestBed.createComponent(UiShowcase);
    fixture.detectChanges();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  it('lists the first page of sample customers', () => {
    const { element } = render();

    expect(element.querySelectorAll('tr[mat-row]').length).toBe(5);
    expect(element.querySelector('.mat-mdc-paginator-range-label')?.textContent).toContain('1 – 5 von 23');
  });

  it('deletes only after confirmation', async () => {
    const { fixture, element } = render();
    confirm.mockResolvedValue(true);

    (element.querySelector('tr[mat-row] button') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(confirm).toHaveBeenCalled();
    expect(notifications.success).toHaveBeenCalledWith('„Anna Bauer“ wurde gelöscht.');
    expect(element.querySelector('.mat-mdc-paginator-range-label')?.textContent).toContain('von 22');
  });

  it('shows validation messages instead of saving an invalid form', async () => {
    const { fixture, element } = render();

    (element.querySelector('form button[type="submit"]') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(notifications.success).not.toHaveBeenCalled();
    expect(element.querySelector('mat-error')?.textContent).toContain('Pflichtfeld');
  });
});
