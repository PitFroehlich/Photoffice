package de.photoffice.tenant;

import static de.photoffice.identity.TestTokens.platformAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.photoffice.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PlatformTenantControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void registersStudioAndListsIt() throws Exception {
		String slug = uniqueSlug();

		create(slug, "Fotostudio Müller").andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.slug").value(slug))
			.andExpect(jsonPath("$.name").value("Fotostudio Müller"))
			.andExpect(jsonPath("$.status").value("ACTIVE"));

		mockMvc.perform(get("/api/platform/tenants").with(platformAdmin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.slug == '%s')]", slug).exists());
	}

	@Test
	void rejectsDuplicateSlug() throws Exception {
		String slug = uniqueSlug();
		create(slug, "First").andExpect(status().isCreated());

		create(slug, "Second").andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void rejectsInvalidSlug() throws Exception {
		create("Not A Slug!", "Studio").andExpect(status().isBadRequest());
	}

	@Test
	void requiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/platform/tenants")).andExpect(status().isUnauthorized());
	}

	private ResultActions create(String slug, String name) throws Exception {
		return mockMvc.perform(post("/api/platform/tenants").with(platformAdmin()).contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"slug": "%s", "name": "%s"}""".formatted(slug, name)));
	}

	private static String uniqueSlug() {
		return "studio-" + UUID.randomUUID().toString().substring(0, 8);
	}

}
