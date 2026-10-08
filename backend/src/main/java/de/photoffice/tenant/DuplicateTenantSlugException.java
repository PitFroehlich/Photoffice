package de.photoffice.tenant;

public class DuplicateTenantSlugException extends RuntimeException {

	public DuplicateTenantSlugException(String slug) {
		super("Das Kürzel „" + slug + "“ ist bereits vergeben.");
	}

}
