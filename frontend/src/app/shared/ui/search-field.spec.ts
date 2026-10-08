import { TestBed } from '@angular/core/testing';
import { iconTesting } from '../../../testing/test-providers';
import { SearchField } from './search-field';

describe('SearchField', () => {
  afterEach(() => vi.useRealTimers());

  it('emits the trimmed term after a pause in typing', async () => {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({
      imports: [SearchField, iconTesting],
    }).compileComponents();
    const fixture = TestBed.createComponent(SearchField);
    const emitted: string[] = [];
    fixture.componentInstance.search.subscribe((term) => emitted.push(term));
    fixture.detectChanges();

    const input = (fixture.nativeElement as HTMLElement).querySelector('input')!;
    input.value = 'Ann';
    input.dispatchEvent(new Event('input'));
    input.value = ' Anna ';
    input.dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(300);

    expect(emitted).toEqual(['Anna']);
  });
});
