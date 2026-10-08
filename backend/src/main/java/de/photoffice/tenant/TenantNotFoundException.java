package de.photoffice.tenant;

public class TenantNotFoundException extends RuntimeException {

	public TenantNotFoundException(TenantId id) {
		super("Kein Studio mit der ID " + id + " gefunden.");
	}

}
