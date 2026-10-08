package de.photoffice.tenant;

public class TenantNotFoundException extends RuntimeException {

	public TenantNotFoundException(TenantId id) {
		super("No tenant with id '" + id + "'");
	}

}
