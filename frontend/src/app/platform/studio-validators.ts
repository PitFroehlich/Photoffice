/**
 * Format rules for registering a studio – identical to the backend (TenantManagement.SLUG_FORMAT, api/openapi.yaml).
 */
export const studioPatterns = {
  /** Lowercase letters, digits and inner hyphens, 3 to 63 characters (usable as subdomain). */
  slug: /^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$/,
  email: /^[^\s@]+@[^\s@]+\.[^\s@.]{2,}$/u,
};

export const studioHints = {
  slug: '3–63 Zeichen: Kleinbuchstaben, Ziffern und Bindestriche, nicht am Anfang oder Ende',
  email: 'Bitte eine gültige E-Mail-Adresse eingeben, z. B. name@beispiel.de',
};

const umlauts: Record<string, string> = { ä: 'ae', ö: 'oe', ü: 'ue', ß: 'ss' };

/** Suggests a slug for a studio name, e.g. "Fotostudio Müller" → "fotostudio-mueller". */
export function suggestSlug(name: string): string {
  return name
    .toLowerCase()
    .replace(/[äöüß]/g, (c) => umlauts[c])
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 63)
    .replace(/-+$/g, '');
}
