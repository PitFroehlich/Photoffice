import { Component, DestroyRef, OnInit, inject, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { debounceTime, distinctUntilChanged, map } from 'rxjs';

/**
 * Search input for lists. Emits the trimmed search term after a short pause in typing (`debounceMs`).
 * Lists should search server-side (query parameter) and reset to the first page on a new term.
 */
@Component({
  selector: 'app-search-field',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatIconModule, MatButtonModule],
  template: `
    <mat-form-field class="search">
      <mat-label>{{ label() }}</mat-label>
      <mat-icon matPrefix svgIcon="search" aria-hidden="true" />
      <input matInput type="search" [formControl]="term" autocomplete="off" />
      @if (term.value) {
        <button matIconButton matSuffix type="button" aria-label="Suche leeren" (click)="term.setValue('')">
          <mat-icon svgIcon="close" />
        </button>
      }
    </mat-form-field>
  `,
  styles: `
    .search {
      width: 100%;
      max-width: 24rem;
    }
  `,
})
export class SearchField implements OnInit {
  readonly label = input('Suchen');
  readonly debounceMs = input(300);
  readonly search = output<string>();

  protected readonly term = new FormControl('', { nonNullable: true });
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.term.valueChanges
      .pipe(
        debounceTime(this.debounceMs()),
        map((value) => value.trim()),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((value) => this.search.emit(value));
  }
}
