import { afterNextRender, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AccountAction, AuthService } from '../auth/auth.service';
import { NotificationService } from '../shared/ui/notification.service';

const successMessages: Record<AccountAction, string> = {
  UPDATE_PASSWORD: 'Ihr Passwort wurde geändert.',
  UPDATE_PROFILE: 'Ihr Profil wurde gespeichert.',
};

/**
 * "Profil bearbeiten" and "Passwort ändern" in the user menu of the studio and platform shell (issue #46, ADR 0013):
 * opens the Keycloak page for the action and confirms the result after returning. Call in an injection context.
 */
export function accountActions() {
  const auth = inject(AuthService);
  const router = inject(Router);
  const notifications = inject(NotificationService);

  afterNextRender(() => {
    const result = auth.takeAccountActionResult();
    if (result?.status === 'success') {
      notifications.success(successMessages[result.action]);
    } else if (result?.status === 'error') {
      notifications.error(
        'Die Änderung konnte nicht gespeichert werden. Bitte versuchen Sie es erneut.',
      );
    }
  });

  return {
    start: (action: AccountAction) => auth.startAccountAction(action, router.url),
  };
}
