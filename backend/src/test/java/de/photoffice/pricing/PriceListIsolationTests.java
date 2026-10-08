package de.photoffice.pricing;

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
 * Studio A's price list is invisible and untouchable for studio B.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PriceListIsolationTests {

	private static final InitialStudioAdmin STUDIO_ADMIN = new InitialStudioAdmin("admin@example.test", null, null);

	private static final String BASE = "/api/studio/price-list";

	private static final String PRINT = """
			{"type": "PRINT", "paperType": "Matt", "printFormat": "13 × 18 cm", "priceCents": 290}""";

	private static final String VARIANT = """
			{"type": "DOWNLOAD", "downloadName": "Original", "priceCents": 990}""";

	private static final String PACKAGE = """
			{"name": "Ganze Galerie", "kind": "WHOLE_GALLERY", "downloadProductId": "%s", "priceCents": 14900}""";

	private static final String SHIPPING = """
			{"name": "Standardversand", "priceCents": 490}""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantManagement tenantManagement;

	private String studioA;

	private String studioB;

	private String productOfA;

	private String variantOfA;

	private String packageOfA;

	private String shippingOfA;

	@BeforeEach
	void createPriceListOfStudioA() throws Exception {
		studioA = tenantManagement.register(uniqueSlug(), "Studio A", STUDIO_ADMIN).slug();
		studioB = tenantManagement.register(uniqueSlug(), "Studio B", STUDIO_ADMIN).slug();
		productOfA = create(studioA, "/products", PRINT);
		variantOfA = create(studioA, "/products", VARIANT);
		packageOfA = create(studioA, "/download-packages", PACKAGE.formatted(variantOfA));
		shippingOfA = create(studioA, "/shipping-methods", SHIPPING);
		mockMvc.perform(put(BASE + "/settings").with(studioAdmin(studioA))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"vatRatePercent": 7}"""))
			.andExpect(status().isOk());
	}

	@Test
	void otherStudioSeesItsOwnEmptyPriceList() throws Exception {
		mockMvc.perform(get(BASE).with(studioAdmin(studioA)))
			.andExpect(jsonPath("$.products.length()").value(2))
			.andExpect(jsonPath("$.downloadPackages.length()").value(1))
			.andExpect(jsonPath("$.shippingMethods.length()").value(1))
			.andExpect(jsonPath("$.settings.vatRatePercent").value(7.0));
		mockMvc.perform(get(BASE).with(studioAdmin(studioB)))
			.andExpect(jsonPath("$.products.length()").value(0))
			.andExpect(jsonPath("$.downloadPackages.length()").value(0))
			.andExpect(jsonPath("$.shippingMethods.length()").value(0))
			.andExpect(jsonPath("$.settings.vatRatePercent").value(19.0));
	}

	@Test
	void otherStudioCannotReadUpdateOrDeleteTheEntries() throws Exception {
		assertUntouchable("/products/" + productOfA, PRINT);
		assertUntouchable("/download-packages/" + packageOfA, PACKAGE.formatted(variantOfA));
		assertUntouchable("/shipping-methods/" + shippingOfA, SHIPPING);

		mockMvc.perform(get(BASE + "/products/" + productOfA).with(studioAdmin(studioA)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.priceCents").value(290));
	}

	@Test
	void sameEntriesMayExistInDifferentStudios() throws Exception {
		create(studioB, "/products", PRINT);
		String variantOfB = create(studioB, "/products", VARIANT);
		create(studioB, "/download-packages", PACKAGE.formatted(variantOfB));
		create(studioB, "/shipping-methods", SHIPPING);
	}

	@Test
	void otherStudioCannotUseTheDownloadVariantOfStudioA() throws Exception {
		mockMvc.perform(post(BASE + "/download-packages").with(studioAdmin(studioB))
			.contentType(MediaType.APPLICATION_JSON)
			.content(PACKAGE.formatted(variantOfA)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Die gewählte Download-Variante gibt es nicht."));
	}

	private void assertUntouchable(String path, String json) throws Exception {
		mockMvc.perform(get(BASE + path).with(studioAdmin(studioB))).andExpect(status().isNotFound());
		mockMvc.perform(put(BASE + path).with(studioAdmin(studioB)).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isNotFound());
		mockMvc.perform(delete(BASE + path).with(studioAdmin(studioB))).andExpect(status().isNotFound());
		mockMvc.perform(get(BASE + path).with(studioAdmin(studioA))).andExpect(status().isOk());
	}

	private String create(String studio, String path, String json) throws Exception {
		String body = mockMvc
			.perform(post(BASE + path).with(studioAdmin(studio)).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private static String uniqueSlug() {
		return "studio-" + UUID.randomUUID().toString().substring(0, 8);
	}

}
