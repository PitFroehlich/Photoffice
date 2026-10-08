package de.photoffice.tenant;

public class DuplicateTenantSlugException extends RuntimeException {

	public DuplicateTenantSlugException(String slug) {
		super("A tenant with slug '" + slug + "' already exists");
	}

}
