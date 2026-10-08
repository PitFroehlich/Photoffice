package de.photoffice;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

	private final ApplicationModules modules = ApplicationModules.of(PhotofficeBackendApplication.class);

	@Test
	void verifiesModuleStructure() {
		modules.verify();
	}

	@Test
	void writesModuleDocumentation() {
		new Documenter(modules).writeDocumentation();
	}

}
