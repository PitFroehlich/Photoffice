package de.photoffice.studioprofile;

import static de.photoffice.identity.TestTokens.photographer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.photoffice.TestcontainersConfiguration;
import de.photoffice.tenant.TenantContext;
import de.photoffice.tenant.TenantId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The dev data (Liquibase context "dev") loads and satisfies the same format rules as user input – otherwise the
 * demo would show values the form rejects.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(TestcontainersConfiguration.class)
class StudioProfileDevDataTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StudioProfileManagement studioProfileManagement;

	@ParameterizedTest
	@ValueSource(strings = { "a0000000-0000-4000-8000-00000000000a", "b0000000-0000-4000-8000-00000000000b" })
	void devProfilesSatisfyTheFormatRules(String tenantId) {
		StudioProfile profile = TenantContext.callAs(TenantId.of(UUID.fromString(tenantId)),
				studioProfileManagement::profile);
		assertThat(profile.updatedAt()).isPresent();

		StudioProfileData revalidated = new StudioProfileData(profile.displayName(), profile.street(),
				profile.postalCode(), profile.city(), profile.country(), profile.email(), profile.phone(),
				profile.website(), profile.taxNumber(), profile.vatId(), profile.accountHolder(), profile.iban(),
				profile.bic());
		assertThat(revalidated.iban()).isEqualTo(profile.iban());
		assertThat(revalidated.vatId()).isEqualTo(profile.vatId());
		assertThat(revalidated.website()).isEqualTo(profile.website());
	}

	@Test
	void studioAHasTermsAndImprint() throws Exception {
		mockMvc.perform(get("/api/studio/profile").with(photographer("studio-a")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("Studio A Fotografie"));
		mockMvc.perform(get("/api/studio/profile/legal-texts").with(photographer("studio-a")))
			.andExpect(jsonPath("$[0].markdown").value(org.hamcrest.Matchers.startsWith("# Allgemeine Geschäftsbedingungen")))
			.andExpect(jsonPath("$[1].markdown").value(""))
			.andExpect(jsonPath("$[2].markdown").value(org.hamcrest.Matchers.startsWith("# Impressum")));
	}

}
