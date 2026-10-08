package de.photoffice.pricing;

/**
 * Kind of a single product. New types (e.g. photo book) get their own attributes, see ADR 0007.
 */
public enum ProductType {

	/** Print of an image: paper type × print format. */
	PRINT,

	/** Image file in a resolution. */
	DOWNLOAD

}
