package de.photoffice.customer;

import static de.photoffice.identity.TestTokens.studioAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import de.photoffice.TestcontainersConfiguration;
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
 * Studio A's customers are invisible and untouchable for studio B (acceptance criterion of #6/#7).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CustomerIsolationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantManagement tenantManagement;

	private String studioA;

	private String studioB;

	private String customerOfA;

	@BeforeEach
	void createCustomerInStudioA() throws Exception {
		studioA = tenantManagement.register(uniqueSlug(), "Studio A").slug();
		studioB = tenantManagement.register(uniqueSlug(), "Studio B").slug();
		String body = mockMvc
			.perform(post("/api/studio/customers").with(studioAdmin(studioA))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"firstName": "Julia", "lastName": "Becker", "email": "julia@example.test"}"""))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		customerOfA = JsonPath.read(body, "$.id");
	}

	@Test
	void otherStudioDoesNotSeeTheCustomerInLists() throws Exception {
		mockMvc.perform(get("/api/studio/customers").with(studioAdmin(studioA)))
			.andExpect(jsonPath("$.totalElements").value(1));
		mockMvc.perform(get("/api/studio/customers").with(studioAdmin(studioB)))
			.andExpect(jsonPath("$.totalElements").value(0));
		mockMvc.perform(get("/api/studio/customers?search=julia").with(studioAdmin(studioB)))
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void otherStudioCannotReadUpdateOrDeleteTheCustomer() throws Exception {
		String url = "/api/studio/customers/" + customerOfA;

		mockMvc.perform(get(url).with(studioAdmin(studioB))).andExpect(status().isNotFound());
		mockMvc.perform(put(url).with(studioAdmin(studioB)).contentType(MediaType.APPLICATION_JSON).content("""
				{"firstName": "Hacked", "lastName": "Becker", "email": "julia@example.test"}"""))
			.andExpect(status().isNotFound());
		mockMvc.perform(delete(url).with(studioAdmin(studioB))).andExpect(status().isNotFound());

		mockMvc.perform(get(url).with(studioAdmin(studioA)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.firstName").value("Julia"));
	}

	@Test
	void sameEmailMayExistInDifferentStudios() throws Exception {
		mockMvc.perform(post("/api/studio/customers").with(studioAdmin(studioB))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"firstName": "Julia", "lastName": "Becker", "email": "julia@example.test"}"""))
			.andExpect(status().isCreated());
	}

	private static String uniqueSlug() {
		return "studio-" + UUID.randomUUID().toString().substring(0, 8);
	}

}
