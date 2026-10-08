package de.photoffice.studioprofile;

import static de.photoffice.identity.TestTokens.photographer;
import static de.photoffice.identity.TestTokens.studioAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StudioProfileApiTests {

	private static final InitialStudioAdmin STUDIO_ADMIN = new InitialStudioAdmin("admin@example.test", null, null);

	private static final String PROFILE = "/api/studio/profile";

	private static final String LEGAL_TEXTS = "/api/studio/profile/legal-texts";

	static final String FULL_PROFILE = """
			{"displayName": " Lichtblick Fotografie ", "street": "Lindenstraße 4", "postalCode": "10969",
			 "city": "Berlin", "country": "DE", "email": "Kontakt@Lichtblick.example", "phone": "030 1234567",
			 "website": "www.lichtblick.example", "taxNumber": "12/345/67890", "vatId": "de 123 456 789",
			 "accountHolder": "Lichtblick Fotografie GmbH", "iban": "DE89 3704 0044 0532 0130 00",
			 "bic": "cobadeffxxx"}""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantManagement tenantManagement;

	private String studio;

	@BeforeEach
	void registerStudio() {
		studio = tenantManagement.register("studio-" + UUID.randomUUID().toString().substring(0, 8), "Studio Sonnenschein",
				STUDIO_ADMIN).slug();
	}

	@Test
	void newStudioHasAnEmptyProfileWithTheStudioName() throws Exception {
		mockMvc.perform(get(PROFILE).with(studioAdmin(studio)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("Studio Sonnenschein"))
			.andExpect(jsonPath("$.country").value("DE"))
			.andExpect(jsonPath("$.street").doesNotExist())
			.andExpect(jsonPath("$.iban").doesNotExist())
			.andExpect(jsonPath("$.updatedAt").doesNotExist());
	}

	@Test
	void savesAndNormalisesTheProfile() throws Exception {
		send(put(PROFILE), FULL_PROFILE).andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("Lichtblick Fotografie"))
			.andExpect(jsonPath("$.email").value("kontakt@lichtblick.example"))
			.andExpect(jsonPath("$.website").value("https://www.lichtblick.example"))
			.andExpect(jsonPath("$.vatId").value("DE123456789"))
			.andExpect(jsonPath("$.iban").value("DE89370400440532013000"))
			.andExpect(jsonPath("$.bic").value("COBADEFFXXX"))
			.andExpect(jsonPath("$.updatedAt").exists());

		mockMvc.perform(get(PROFILE).with(studioAdmin(studio)))
			.andExpect(jsonPath("$.street").value("Lindenstraße 4"))
			.andExpect(jsonPath("$.postalCode").value("10969"))
			.andExpect(jsonPath("$.taxNumber").value("12/345/67890"))
			.andExpect(jsonPath("$.accountHolder").value("Lichtblick Fotografie GmbH"));

		// Optional fields can be removed again
		send(put(PROFILE), """
				{"displayName": "Lichtblick", "country": "AT", "postalCode": "1060", "phone": ""}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.country").value("AT"))
			.andExpect(jsonPath("$.postalCode").value("1060"))
			.andExpect(jsonPath("$.phone").doesNotExist())
			.andExpect(jsonPath("$.iban").doesNotExist());
	}

	@Test
	void rejectsInvalidValuesWithGermanMessages() throws Exception {
		send(put(PROFILE), """
				{"displayName": "Lichtblick", "country": "DE", "postalCode": "1060"}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("PLZ: 5 Ziffern für Deutschland"));
		send(put(PROFILE), """
				{"displayName": "Lichtblick", "country": "DE", "accountHolder": "Lichtblick",
				 "iban": "DE89370400440532013001"}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail")
				.value("IBAN: bitte eine gültige IBAN angeben (Prüfsumme), z. B. DE89 3704 0044 0532 0130 00"));
		send(put(PROFILE), """
				{"displayName": "Lichtblick", "country": "DE", "iban": "DE89370400440532013000"}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Bankverbindung: Kontoinhaber und IBAN bitte gemeinsam angeben"));
		send(put(PROFILE), """
				{"displayName": "  ", "country": "DE"}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Anzeigename ist ein Pflichtfeld"));
		send(put(PROFILE), """
				{"displayName": "Lichtblick", "country": "FR"}""").andExpect(status().isBadRequest());
		send(put(PROFILE), """
				{"displayName": "Lichtblick"}""").andExpect(status().isBadRequest());

		mockMvc.perform(get(PROFILE).with(studioAdmin(studio))).andExpect(jsonPath("$.updatedAt").doesNotExist());
	}

	@Test
	void newStudioHasAllLegalTextsEmpty() throws Exception {
		mockMvc.perform(get(LEGAL_TEXTS).with(studioAdmin(studio)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(4))
			.andExpect(jsonPath("$[0].kind").value("TERMS_AND_CONDITIONS"))
			.andExpect(jsonPath("$[1].kind").value("CANCELLATION_POLICY"))
			.andExpect(jsonPath("$[2].kind").value("IMPRINT"))
			.andExpect(jsonPath("$[3].kind").value("PRIVACY_POLICY"))
			.andExpect(jsonPath("$[0].markdown").value(""))
			.andExpect(jsonPath("$[0].updatedAt").doesNotExist());
	}

	@Test
	void savesChangesAndRemovesALegalText() throws Exception {
		send(put(LEGAL_TEXTS + "/IMPRINT"), """
				{"markdown": "\\n# Impressum\\r\\n\\n**Lichtblick**  \\n"}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.kind").value("IMPRINT"))
			.andExpect(jsonPath("$.markdown").value("# Impressum\n\n**Lichtblick**"))
			.andExpect(jsonPath("$.updatedAt").exists());
		send(put(LEGAL_TEXTS + "/IMPRINT"), """
				{"markdown": "# Impressum\\n\\nNeu"}""").andExpect(status().isOk());

		mockMvc.perform(get(LEGAL_TEXTS).with(studioAdmin(studio)))
			.andExpect(jsonPath("$[2].markdown").value("# Impressum\n\nNeu"))
			.andExpect(jsonPath("$[0].markdown").value(""));

		send(put(LEGAL_TEXTS + "/IMPRINT"), """
				{"markdown": "   "}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.markdown").value(""))
			.andExpect(jsonPath("$.updatedAt").doesNotExist());
		mockMvc.perform(get(LEGAL_TEXTS).with(studioAdmin(studio))).andExpect(jsonPath("$[2].markdown").value(""));
	}

	@Test
	void rejectsInvalidLegalTexts() throws Exception {
		send(put(LEGAL_TEXTS + "/IMPRINT"), """
				{"markdown": "AGB\\u0000"}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Der Text enthält unzulässige Steuerzeichen."));
		send(put(LEGAL_TEXTS + "/IMPRINT"), "{\"markdown\": \"" + "a".repeat(50_001) + "\"}")
			.andExpect(status().isBadRequest());
		send(put(LEGAL_TEXTS + "/SOMETHING_ELSE"), """
				{"markdown": "Text"}""").andExpect(status().isBadRequest());
		send(put(LEGAL_TEXTS + "/IMPRINT"), "{}").andExpect(status().isBadRequest());
	}

	@Test
	void photographersMayReadButNotChange() throws Exception {
		send(put(PROFILE), FULL_PROFILE).andExpect(status().isOk());
		send(put(LEGAL_TEXTS + "/TERMS_AND_CONDITIONS"), """
				{"markdown": "# AGB"}""").andExpect(status().isOk());

		mockMvc.perform(get(PROFILE).with(photographer(studio)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("Lichtblick Fotografie"));
		mockMvc.perform(get(LEGAL_TEXTS).with(photographer(studio)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].markdown").value("# AGB"));

		mockMvc.perform(put(PROFILE).with(photographer(studio))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"displayName": "Übernommen", "country": "DE"}"""))
			.andExpect(status().isForbidden());
		mockMvc.perform(put(LEGAL_TEXTS + "/TERMS_AND_CONDITIONS").with(photographer(studio))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"markdown": "geändert"}"""))
			.andExpect(status().isForbidden());

		mockMvc.perform(get(PROFILE).with(studioAdmin(studio)))
			.andExpect(jsonPath("$.displayName").value("Lichtblick Fotografie"));
		mockMvc.perform(get(LEGAL_TEXTS).with(studioAdmin(studio))).andExpect(jsonPath("$[0].markdown").value("# AGB"));
	}

	@Test
	void requiresAStudioLogin() throws Exception {
		mockMvc.perform(get(PROFILE)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(LEGAL_TEXTS)).andExpect(status().isUnauthorized());
	}

	private ResultActions send(MockHttpServletRequestBuilder request, String json) throws Exception {
		return mockMvc.perform(request.with(studioAdmin(studio)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

}
