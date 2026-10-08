import { LegalTextKind, StudioProfile } from '../api/models';
import { countryLabels } from './studio-profile-validators';

export interface LegalTextLabel {
  /** Short label for the tab */
  tab: string;
  /** Full name of the text */
  title: string;
}

export const legalTextLabels: Record<LegalTextKind, LegalTextLabel> = {
  TERMS_AND_CONDITIONS: { tab: 'AGB', title: 'Allgemeine Geschäftsbedingungen' },
  CANCELLATION_POLICY: { tab: 'Widerruf', title: 'Widerrufsbelehrung' },
  IMPRINT: { tab: 'Impressum', title: 'Impressum' },
  PRIVACY_POLICY: { tab: 'Datenschutz', title: 'Datenschutzerklärung' },
};

export const legalTextKinds = Object.keys(legalTextLabels) as LegalTextKind[];

/** Same limits as LegalText.java: at most 50,000 characters, no control characters except tab and line breaks. */
export const LEGAL_TEXT_MAX_LENGTH = 50_000;
export const legalTextPattern = /^[^\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]*$/u;
export const legalTextHint = 'Der Text enthält unzulässige Steuerzeichen.';

/**
 * Outline suggestions for an empty text: headings with placeholders in [brackets]. They structure the text, they
 * are not legal advice – the page says so.
 */
export function legalTextOutline(kind: LegalTextKind, profile?: StudioProfile): string {
  switch (kind) {
    case 'TERMS_AND_CONDITIONS':
      return [
        '# Allgemeine Geschäftsbedingungen',
        '',
        '## 1. Geltungsbereich',
        '[Für welche Leistungen und Bestellungen gelten diese AGB?]',
        '',
        '## 2. Vertragsschluss',
        '[Wie kommt der Vertrag bei einer Bestellung in der Online-Galerie zustande?]',
        '',
        '## 3. Preise und Zahlung',
        '[Preise, Versandkosten, Zahlungsarten]',
        '',
        '## 4. Lieferung und Downloads',
        '[Lieferzeiten für Abzüge, Bereitstellung von Downloads]',
        '',
        '## 5. Nutzungsrechte an den Bildern',
        '[Welche Nutzung der gekauften Bilder ist erlaubt?]',
        '',
        '## 6. Gewährleistung und Haftung',
        '[…]',
        '',
        '## 7. Schlussbestimmungen',
        '[…]',
      ].join('\n');
    case 'CANCELLATION_POLICY':
      return [
        '# Widerrufsbelehrung',
        '',
        '## Widerrufsrecht',
        '[Ihre Widerrufsbelehrung]',
        '',
        '## Folgen des Widerrufs',
        '[…]',
        '',
        '## Ausschluss oder vorzeitiges Erlöschen des Widerrufsrechts',
        '[Falls zutreffend, z. B. für Downloads oder individuell angefertigte Abzüge]',
        '',
        '## Muster-Widerrufsformular',
        '[…]',
      ].join('\n');
    case 'IMPRINT':
      return imprintOutline(profile);
    case 'PRIVACY_POLICY':
      return [
        '# Datenschutzerklärung',
        '',
        '## 1. Verantwortlicher',
        '[Name und Kontaktdaten des Studios]',
        '',
        '## 2. Welche Daten wir verarbeiten',
        '[z. B. Name, E-Mail-Adresse, Bestelldaten, Zugriffe auf die Online-Galerie]',
        '',
        '## 3. Zwecke und Rechtsgrundlagen',
        '[…]',
        '',
        '## 4. Empfänger',
        '[z. B. Druckdienstleister, Zahlungsanbieter, Hosting]',
        '',
        '## 5. Speicherdauer',
        '[…]',
        '',
        '## 6. Ihre Rechte',
        '[Auskunft, Berichtigung, Löschung, Einschränkung, Widerspruch, Datenübertragbarkeit, Beschwerde]',
      ].join('\n');
  }
}

/** Imprint outline filled with the studio profile (only the fields that are set). */
function imprintOutline(profile?: StudioProfile): string {
  const lines = (...values: (string | undefined | false)[]) =>
    values.filter((value): value is string => !!value);
  const place = [profile?.postalCode, profile?.city].filter(Boolean).join(' ');
  return [
    '# Impressum',
    '',
    ...lines(
      profile ? `**${profile.displayName}**` : '**[Name des Studios]**',
      profile?.street ?? '[Straße und Hausnummer]',
      place || '[PLZ Ort]',
      profile && countryLabels[profile.country],
    ),
    '',
    ...lines(
      profile?.phone && `Telefon: ${profile.phone}`,
      profile?.email && `E-Mail: ${profile.email}`,
      profile?.website && `Website: ${profile.website}`,
    ),
    ...(profile?.vatId ? ['', `Umsatzsteuer-ID: ${profile.vatId}`] : []),
    '',
    '[Vertretungsberechtigte Person, Registereintrag, Aufsichtsbehörde o. Ä. – soweit für Ihr Studio zutreffend]',
  ].join('\n');
}
