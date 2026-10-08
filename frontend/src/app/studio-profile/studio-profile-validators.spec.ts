import {
  isValidIban,
  postalCodePatterns,
  studioProfilePatterns,
} from './studio-profile-validators';

// Same cases as StudioProfileDataTests.java – frontend and backend must agree
describe('studioProfilePatterns', () => {
  const accepted: [keyof typeof studioProfilePatterns, string][] = [
    ['name', 'Lichtblick Fotografie'],
    ['name', 'Foto & Design GmbH'],
    ['name', 'Studio 21'],
    ['name', '21 Gramm Fotografie'],
    ['name', 'Foto-Atelier Müller'],
    ['name', 'Studio A Fotografie GmbH'],
    ['street', 'Lindenstraße 4'],
    ['street', 'Am Markt 1/2'],
    ['street', 'Straße des 17. Juni 135'],
    ['city', 'Frankfurt (Oder)'],
    ['city', 'Zürich'],
    ['email', 'kontakt@studio.example.de'],
    ['phone', '+43 1 2345678'],
    ['phone', '030 1234567'],
    ['website', 'studio.de'],
    ['website', 'www.studio-a.de'],
    ['website', 'https://www.studio-a.de/kontakt'],
    ['website', 'http://fotografie-müller.de'],
    ['website', 'https://studio.de:8443/'],
    ['taxNumber', '12/345/67890'],
    ['taxNumber', '2181508150'],
    ['taxNumber', '21 815 08150'],
    ['taxNumber', '12-345/6789'],
    ['taxNumber', '1121081508150'],
    ['vatId', 'DE123456789'],
    ['vatId', 'de 123 456 789'],
    ['vatId', 'ATU12345678'],
    ['vatId', 'CHE-123.456.789 MWST'],
    ['vatId', 'CHE123456789'],
    ['vatId', 'che-123.456.789 tva'],
    ['bic', 'COBADEFFXXX'],
    ['bic', 'cobadeff'],
    ['bic', 'GIBAATWW'],
  ];

  const rejected: [keyof typeof studioProfilePatterns, string][] = [
    ['name', '123'],
    ['name', '-Studio'],
    ['name', 'Studio <b>'],
    ['name', 'Studio!'],
    ['name', '@home'],
    ['name', '!!!'],
    ['street', '12345'],
    ['street', 'Lindenstraße'],
    ['city', '10115 Berlin'],
    ['email', 'kontakt@studio'],
    ['phone', 'abc'],
    ['website', 'studio'],
    ['website', 'ftp://studio.de'],
    ['website', 'https://'],
    ['website', 'studio .de'],
    ['website', 'javascript:alert(1)'],
    ['taxNumber', '1234567'],
    ['taxNumber', '12345678901234'],
    ['taxNumber', '12/345/6789a'],
    ['taxNumber', '/12345678'],
    ['vatId', 'DE12345678'],
    ['vatId', 'ATU1234567'],
    ['vatId', 'AT12345678'],
    ['vatId', 'FR12345678901'],
    ['vatId', 'CHE-123.456.78'],
    ['bic', 'COBADEF'],
    ['bic', 'COBA1EFFXXX'],
    ['bic', 'COBADEFFXX'],
  ];

  it('accepts realistic values', () => {
    for (const [field, value] of accepted) {
      expect(studioProfilePatterns[field].test(value), `${field}: ${value}`).toBe(true);
    }
  });

  it('rejects garbage', () => {
    for (const [field, value] of rejected) {
      expect(studioProfilePatterns[field].test(value), `${field}: ${value}`).toBe(false);
    }
  });

  it('checks postal codes per country', () => {
    expect(postalCodePatterns.DE.test('10969')).toBe(true);
    expect(postalCodePatterns.DE.test('04109')).toBe(true);
    expect(postalCodePatterns.AT.test('1060')).toBe(true);
    expect(postalCodePatterns.CH.test('8001')).toBe(true);
    expect(postalCodePatterns.DE.test('1060')).toBe(false);
    expect(postalCodePatterns.AT.test('10969')).toBe(false);
    expect(postalCodePatterns.CH.test('ABCD')).toBe(false);
    expect(postalCodePatterns.DE.test('123456')).toBe(false);
  });
});

describe('isValidIban', () => {
  it('accepts IBANs with a valid checksum, with spaces and in lower case', () => {
    for (const iban of [
      'DE89370400440532013000',
      'DE89 3704 0044 0532 0130 00',
      'de89 3704 0044 0532 0130 00',
      'AT611904300234573201',
      'CH9300762011623852957',
    ]) {
      expect(isValidIban(iban), iban).toBe(true);
    }
  });

  it('rejects wrong checksums, lengths and characters', () => {
    for (const iban of [
      'DE89370400440532013001',
      'DE8937040044053201300',
      'AT611904300234573202',
      'XX00',
      'DE89-3704-0044',
    ]) {
      expect(isValidIban(iban), iban).toBe(false);
    }
  });
});
