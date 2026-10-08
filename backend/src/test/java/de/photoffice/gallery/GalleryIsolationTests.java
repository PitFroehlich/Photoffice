package de.photoffice.gallery;

import static de.photoffice.identity.TestTokens.studioAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
 * Studio A's galleries and customers are invisible and untouchable for studio B.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GalleryIsolationTests {

	private static final InitialStudioAdmin STUDIO_ADMIN = new InitialStudioAdmin("admin@example.test", null, null);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantManagement tenantManagement;

	private String studioA;

	private String studioB;

	private String customerOfA;

	private String galleryOfA;

	@BeforeEach
	void createGalleryInStudioA() throws Exception {
		studioA = tenantManagement.register(uniqueSlug(), "Studio A", STUDIO_ADMIN).slug();
		studioB = tenantManagement.register(uniqueSlug(), "Studio B", STUDIO_ADMIN).slug();
		customerOfA = JsonPath.read(createAs(studioA, "/api/studio/customers", """
				{"firstName": "Julia", "lastName": "Becker", "email": "julia@example.test"}"""), "$.id");
		galleryOfA = JsonPath.read(createAs(studioA, "/api/studio/galleries", """
				{"name": "Hochzeit Becker", "customerIds": ["%s"]}""".formatted(customerOfA)), "$.id");
	}

	@Test
	void otherStudioDoesNotSeeTheGallery() throws Exception {
		mockMvc.perform(get("/api/studio/galleries").with(studioAdmin(studioB)))
			.andExpect(jsonPath("$.totalElements").value(0));
		mockMvc.perform(get("/api/studio/galleries?customerId=" + customerOfA).with(studioAdmin(studioB)))
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void otherStudioCannotReadChangePublishOrDeleteTheGallery() throws Exception {
		String url = "/api/studio/galleries/" + galleryOfA;
		mockMvc.perform(get(url).with(studioAdmin(studioB))).andExpect(status().isNotFound());
		mockMvc.perform(put(url).with(studioAdmin(studioB)).contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "Übernommen", "customerIds": []}""")).andExpect(status().isNotFound());
		mockMvc.perform(post(url + "/publish").with(studioAdmin(studioB))).andExpect(status().isNotFound());
		mockMvc.perform(post(url + "/unpublish").with(studioAdmin(studioB))).andExpect(status().isNotFound());
		mockMvc.perform(delete(url).with(studioAdmin(studioB))).andExpect(status().isNotFound());

		mockMvc.perform(get(url).with(studioAdmin(studioA)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Hochzeit Becker"));
	}

	@Test
	void otherStudioCannotAssignCustomersOfStudioA() throws Exception {
		mockMvc.perform(post("/api/studio/galleries").with(studioAdmin(studioB))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Fremder Kunde", "customerIds": ["%s"]}""".formatted(customerOfA)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Ein ausgewählter Kunde existiert nicht."));
	}

	private String createAs(String studio, String url, String json) throws Exception {
		return mockMvc
			.perform(post(url)
				.with(studioAdmin(studio))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private static String uniqueSlug() {
		return "studio-" + UUID.randomUUID().toString().substring(0, 8);
	}

}
