/** Same rules as the backend (GalleryData.java). */
export const galleryPatterns = {
  name: /^(?=.*\p{L})[\p{L}0-9 .,'&+()/:%-]+$/u,
};

export const galleryHints = {
  name: "Mindestens ein Buchstabe; Buchstaben, Ziffern, Leerzeichen und . , ' & + ( ) / : % -",
};
