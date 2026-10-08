import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { Api } from '../api/api';
import { formatDateTime } from './format-date-time';
import { getStudioProfile, listLegalTexts, updateLegalText } from '../api/functions';
import { LegalText, LegalTextKind, StudioProfile } from '../api/models';
import {
  EmptyState,
  FieldError,
  LoadingIndicator,
  MarkdownView,
  NotificationService,
  PageHeader,
  trimmedPattern,
} from '../shared/ui';
import {
  LEGAL_TEXT_MAX_LENGTH,
  legalTextHint,
  legalTextKinds,
  legalTextLabels,
  legalTextOutline,
  legalTextPattern,
} from './legal-texts';
import { canEditStudioProfile } from './studio-profile-form';

const textValidators = [
  Validators.maxLength(LEGAL_TEXT_MAX_LENGTH),
  trimmedPattern(legalTextPattern),
];

/**
 * Legal texts of the studio (/studio/rechtstexte): one tab per text, Markdown editor with live preview for studio
 * administrators, the rendered text for everybody else.
 */
@Component({
  selector: 'app-legal-texts-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTabsModule,
    EmptyState,
    FieldError,
    LoadingIndicator,
    MarkdownView,
    PageHeader,
  ],
  templateUrl: './legal-texts-page.html',
  styleUrl: './legal-texts-page.scss',
})
export class LegalTextsPage implements OnInit {
  private readonly api = inject(Api);
  private readonly notifications = inject(NotificationService);

  protected readonly kinds = legalTextKinds;
  protected readonly labels = legalTextLabels;
  protected readonly maxLength = LEGAL_TEXT_MAX_LENGTH;
  protected readonly patternHint = legalTextHint;
  protected readonly canEdit = canEditStudioProfile();
  protected readonly formatDateTime = formatDateTime;
  protected readonly loading = signal(true);
  protected readonly saving = signal<LegalTextKind | undefined>(undefined);

  /** Last saved state per text (from the server). */
  protected readonly saved = signal<Partial<Record<LegalTextKind, LegalText>>>({});
  private profile?: StudioProfile;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    TERMS_AND_CONDITIONS: ['', textValidators],
    CANCELLATION_POLICY: ['', textValidators],
    IMPRINT: ['', textValidators],
    PRIVACY_POLICY: ['', textValidators],
  });

  /** Current editor content – drives the live preview. */
  protected readonly values = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  protected readonly unsaved = computed(() => {
    const values = this.values();
    const saved = this.saved();
    return new Set(
      this.kinds.filter(
        (kind) => (values[kind] ?? '').trim() !== (saved[kind]?.markdown ?? '').trim(),
      ),
    );
  });

  async ngOnInit(): Promise<void> {
    try {
      const [texts, profile] = await Promise.all([
        this.api.invoke(listLegalTexts),
        this.api.invoke(getStudioProfile),
      ]);
      this.profile = profile;
      this.saved.set(Object.fromEntries(texts.map((text) => [text.kind, text])));
      this.form.reset(Object.fromEntries(texts.map((text) => [text.kind, text.markdown])));
    } catch (error) {
      this.notifications.error(error);
    } finally {
      this.loading.set(false);
    }
  }

  protected value(kind: LegalTextKind): string {
    return this.values()[kind] ?? '';
  }

  protected placeholder(kind: LegalTextKind): string {
    return `# ${this.labels[kind].title}\n\n## 1. Abschnitt\nText …`;
  }

  protected insertOutline(kind: LegalTextKind): void {
    const control = this.form.controls[kind];
    control.setValue(legalTextOutline(kind, this.profile));
    control.markAsDirty();
  }

  protected async save(kind: LegalTextKind): Promise<void> {
    const control = this.form.controls[kind];
    if (control.invalid) {
      control.markAsDirty();
      return;
    }
    const title = this.labels[kind].title;
    this.saving.set(kind);
    try {
      const text = await this.api.invoke(updateLegalText, {
        kind,
        body: { markdown: control.value },
      });
      this.saved.update((saved) => ({ ...saved, [kind]: text }));
      control.reset(text.markdown);
      this.notifications.success(
        text.markdown ? `„${title}“ wurde gespeichert.` : `„${title}“ wurde entfernt.`,
      );
    } catch (error) {
      this.notifications.error(error);
    } finally {
      this.saving.set(undefined);
    }
  }
}
