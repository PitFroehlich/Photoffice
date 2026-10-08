import { priceListPatterns } from './price-list-validators';

// Same cases as PriceListTextsTests.java – frontend and backend must agree
describe('priceListPatterns', () => {
  it('accepts print formats in cm and DIN A0–A6', () => {
    for (const value of [
      '13x18',
      '13 x 18',
      '13 X 18 cm',
      '13×18cm',
      '13*18',
      '10,5 x 15',
      '10.5x15',
      '20,0 x 30',
      'A4',
      'din a4',
      'DIN A3',
      'DIN  A 0',
      '13 × 18 CM',
    ]) {
      expect(priceListPatterns.printFormat.test(value), value).toBe(true);
    }
  });

  it('rejects other print formats', () => {
    for (const value of [
      '13',
      'groß',
      '13 x',
      'x 18',
      '13 x 18 x 5',
      '0 x 18',
      '13 x 0',
      '1000 x 20',
      '13 x 18 mm',
      'A7',
      'DIN B4',
      '13 / 18',
      '<script>',
    ]) {
      expect(priceListPatterns.printFormat.test(value), value).toBe(false);
    }
  });

  it('checks paper types', () => {
    for (const value of [
      'Glänzend',
      'Fine Art Baryt',
      'Hahnemühle Photo Rag 308',
      'Seidenmatt (Lustre)',
      'Metallic/Perlmutt',
    ]) {
      expect(priceListPatterns.paperType.test(value), value).toBe(true);
    }
    for (const value of ['308', '!!!', '-Matt', 'Matt#1', '<b>Matt</b>']) {
      expect(priceListPatterns.paperType.test(value), value).toBe(false);
    }
  });

  it('checks names of packages and shipping methods', () => {
    for (const value of [
      '10 Downloads',
      'Ganze Galerie',
      'DHL Paket',
      'Express (24 h)',
      'Versand: Standard',
      '50% Rabatt-Paket',
    ]) {
      expect(priceListPatterns.name.test(value), value).toBe(true);
    }
    for (const value of ['123', '!!!', '10 #Downloads', '<script>', '€€€']) {
      expect(priceListPatterns.name.test(value), value).toBe(false);
    }
  });
});
