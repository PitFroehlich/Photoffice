package de.photoffice.studioprofile;

import static de.photoffice.identity.TestTokens.studioAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.photoffice.TestcontainersConfiguration;
import de.photoffice.tenant.InitialStudioAdmin;
import de.photoffice.tenant.TenantManagement;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Studio A's profile and legal texts are invisible and untouchable for studio B.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StudioProfileIsolationTests {

	private static final InitialStudioAdmin STUDIO_ADMIN = new InitialStudioAdmin("admin@example.test", null, null);

	private static final String PROFILE = "/api/studio/profile";

	private static final String LEGAL_TEXTS = "/api/studio/profile/legal-texts";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantManagement tenantManagement;

	private String studioA;

	private String studioB;

	@BeforeEach
	void createProfileOfStudioA() throws Exception {
		studioA = tenantManagement.register(uniqueSlug(), "Studio A", STUDIO_ADMIN).slug();
		studioB = tenantManagement.register(uniqueSlug(), "Studio B", STUDIO_ADMIN).slug();
		put(studioA, PROFILE, StudioProfileApiTests.FULL_PROFILE);
		put(studioA, LEGAL_TEXTS + "/TERMS_AND_CONDITIONS", """
				{"markdown": "# AGB von A"}""");
	}

	@Test
	void otherStudioSeesItsOwnEmptyProfileAndTexts() throws Exception {
		mockMvc.perform(get(PROFILE).with(studioAdmin(studioB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("Studio B"))
			.andExpect(jsonPath("$.iban").doesNotExist())
			.andExpect(jsonPath("$.updatedAt").doesNotExist());
		mockMvc.perform(get(LEGAL_TEXTS).with(studioAdmin(studioB))).andExpect(jsonPath("$[0].markdown").value(""));
	}

	@Test
	void changesOfStudioBDoNotTouchStudioA() throws Exception {
		put(studioB, PROFILE, """
				{"displayName": "Studio B", "country": "CH", "postalCode": "8001"}""");
		put(studioB, LEGAL_TEXTS + "/TERMS_AND_CONDITIONS", """
				{"markdown": "# AGB von B"}""");
		put(studioB, LEGAL_TEXTS + "/TERMS_AND_CONDITIONS", """
				{"markdown": ""}""");

		mockMvc.perform(get(PROFILE).with(studioAdmin(studioA)))
			.andExpect(jsonPath("$.displayName").value("Lichtblick Fotografie"))
			.andExpect(jsonPath("$.country").value("DE"))
			.andExpect(jsonPath("$.iban").value("DE89370400440532013000"));
		mockMvc.perform(get(LEGAL_TEXTS).with(studioAdmin(studioA)))
			.andExpect(jsonPath("$[0].markdown").value("# AGB von A"));
		mockMvc.perform(get(PROFILE).with(studioAdmin(studioB))).andExpect(jsonPath("$.country").value("CH"));
	}

	private void put(String studio, String path, String json) throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path)
			.with(studioAdmin(studio))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json)).andExpect(status().isOk());
	}

	private static String uniqueSlug() {
		return "studio-" + UUID.randomUUID().toString().substring(0, 8);
	}

}
