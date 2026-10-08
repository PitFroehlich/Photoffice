package de.photoffice.pricing;

import static de.photoffice.identity.TestTokens.photographer;
import static de.photoffice.identity.TestTokens.studioAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import de.photoffice.TestcontainersConfiguration;
import de.photoffice.tenant.Tenant;
import de.photoffice.tenant.TenantContext;
import de.photoffice.tenant.InitialStudioAdmin;
import de.photoffice.tenant.TenantManagement;
import java.math.BigDecimal;
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
class PriceListApiTests {

	private static final InitialStudioAdmin STUDIO_ADMIN = new InitialStudioAdmin("admin@example.test", null, null);

	private static final String BASE = "/api/studio/price-list";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantManagement tenantManagement;

	@Autowired
	private PriceListManagement priceListManagement;

	private Tenant tenant;

	private String studio;

	@BeforeEach
	void registerStudio() {
		tenant = tenantManagement.register("studio-" + UUID.randomUUID().toString().substring(0, 8), "Studio", STUDIO_ADMIN);
		studio = tenant.slug();
	}

	@Test
	void newStudioHasAnEmptyPriceListWithDefaultSettings() throws Exception {
		mockMvc.perform(get(BASE).with(studioAdmin(studio)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.settings.currency").value("EUR"))
			.andExpect(jsonPath("$.settings.vatRatePercent").value(19.0))
			.andExpect(jsonPath("$.products.length()").value(0))
			.andExpect(jsonPath("$.downloadPackages.length()").value(0))
			.andExpect(jsonPath("$.shippingMethods.length()").value(0));
	}

	@Test
	void changesTheVatRate() throws Exception {
		send(put(BASE + "/settings"), """
				{"vatRatePercent": 7}""").andExpect(status().isOk())
			.andExpect(jsonPath("$.vatRatePercent").value(7.0));
		send(put(BASE + "/settings"), """
				{"vatRatePercent": 0}""").andExpect(status().isOk());

		mockMvc.perform(get(BASE).with(studioAdmin(studio))).andExpect(jsonPath("$.settings.vatRatePercent").value(0.0));

		send(put(BASE + "/settings"), """
				{"vatRatePercent": 19.123}""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(
					"Der Steuersatz muss zwischen 0 und 99,99 % liegen (höchstens zwei Nachkommastellen)."));
		send(put(BASE + "/settings"), """
				{"vatRatePercent": -1}""").andExpect(status().isBadRequest());
		send(put(BASE + "/settings"), """
				{"vatRatePercent": 100}""").andExpect(status().isBadRequest());
	}

	@Test
	void createsReadsUpdatesAndDeletesAPrint() throws Exception {
		String location = send(post(BASE + "/products"), """
				{"type": "PRINT", "paperType": " Matt ", "printFormat": "13 × 18 cm", "priceCents": 290}""")
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.paperType").value("Matt"))
			.andExpect(jsonPath("$.priceCents").value(290))
			.andExpect(jsonPath("$.active").value(true))
			.andExpect(jsonPath("$.downloadName").doesNotExist())
			.andReturn()
			.getResponse()
			.getHeader("Location");

		mockMvc.perform(get(location).with(studioAdmin(studio)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.type").value("PRINT"))
			.andExpect(jsonPath("$.printFormat").value("13 × 18 cm"));

		send(put(location), """
				{"type": "PRINT", "paperType": "Matt", "printFormat": "13 × 18 cm", "priceCents": 350, "active": false}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.priceCents").value(350))
			.andExpect(jsonPath("$.active").value(false));

		mockMvc.perform(delete(location).with(studioAdmin(studio))).andExpect(status().isNoContent());
		mockMvc.perform(get(location).with(studioAdmin(studio)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Das Produkt wurde nicht gefunden."));
	}

	@Test
	void createsFreelyNamedDownloadVariants() throws Exception {
		createProduct("""
				{"type": "DOWNLOAD", "downloadName": "Social Media 1080 px", "maxEdgePx": 1080, "priceCents": 290}""");
		createProduct("""
				{"type": "DOWNLOAD", "downloadName": "Web 2048 px", "maxEdgePx": 2048, "priceCents": 490}""");
		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "downloadName": "Original", "priceCents": 990}""")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.maxEdgePx").doesNotExist());

		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "downloadName": "ORIGINAL", "priceCents": 1290}""")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Die Download-Variante „ORIGINAL“ gibt es bereits."));
		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "downloadName": "Mini", "maxEdgePx": 100, "priceCents": 90}""")
			.andExpect(status().isBadRequest());
		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "downloadName": "123", "priceCents": 90}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Name der Download-Variante:")));
	}

	@Test
	void eachPaperAndFormatCombinationExistsOnceIgnoringCase() throws Exception {
		createProduct("""
				{"type": "PRINT", "paperType": "Matt", "printFormat": "13 × 18 cm", "priceCents": 290}""");
		String glossy = createProduct("""
				{"type": "PRINT", "paperType": "Glänzend", "printFormat": "13 × 18 cm", "priceCents": 290}""");

		send(post(BASE + "/products"), """
				{"type": "PRINT", "paperType": "MATT", "printFormat": "13 × 18 CM", "priceCents": 100}""")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Den Abzug „MATT, 13 × 18 cm“ gibt es bereits."));
		// Differently written but equal formats are the same print
		send(post(BASE + "/products"), """
				{"type": "PRINT", "paperType": "Matt", "printFormat": "13x18", "priceCents": 100}""")
			.andExpect(status().isConflict());
		send(post(BASE + "/products"), """
				{"type": "PRINT", "paperType": "Matt", "printFormat": "13", "priceCents": 100}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Format: Breite x Höhe in cm (z. B. 13 x 18 oder 10,5 x 15) oder DIN A0 bis A6"));
		send(put(BASE + "/products/" + glossy), """
				{"type": "PRINT", "paperType": "matt", "printFormat": "13 × 18 cm", "priceCents": 290}""")
			.andExpect(status().isConflict());
		send(put(BASE + "/products/" + glossy), """
				{"type": "PRINT", "paperType": "Glänzend", "printFormat": "13 × 18 cm", "priceCents": 310}""")
			.andExpect(status().isOk());
	}

	@Test
	void productFieldsMustMatchTheType() throws Exception {
		send(post(BASE + "/products"), """
				{"type": "PRINT", "paperType": "Matt", "priceCents": 290}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Für einen Abzug ist das Format Pflicht."));
		send(post(BASE + "/products"), """
				{"type": "PRINT", "paperType": "Matt", "printFormat": "10 × 15 cm", "downloadName": "Web", "priceCents": 290}""")
			.andExpect(status().isBadRequest());
		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "priceCents": 290}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Für einen Download ist der Name der Variante Pflicht."));
		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "downloadName": "Web", "paperType": "Matt", "priceCents": 290}""")
			.andExpect(status().isBadRequest());
		send(post(BASE + "/products"), """
				{"type": "BOOK", "priceCents": 290}""").andExpect(status().isBadRequest());

		String print = createProduct("""
				{"type": "PRINT", "paperType": "Matt", "printFormat": "10 × 15 cm", "priceCents": 190}""");
		send(put(BASE + "/products/" + print), """
				{"type": "DOWNLOAD", "downloadName": "Web", "priceCents": 190}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Der Produkttyp kann nicht geändert werden."));
	}

	@Test
	void pricesAreWholeCentsWithinLimits() throws Exception {
		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "downloadName": "Web", "priceCents": -1}""").andExpect(status().isBadRequest());
		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "downloadName": "Web", "priceCents": 10000001}""").andExpect(status().isBadRequest());
		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "downloadName": "Web", "priceCents": 4.9}""").andExpect(status().isBadRequest());
		send(post(BASE + "/shipping-methods"), """
				{"name": "Abholung im Studio", "priceCents": 0}""").andExpect(status().isCreated());
	}

	@Test
	void managesDownloadPackages() throws Exception {
		String original = createProduct("""
				{"type": "DOWNLOAD", "downloadName": "Original", "priceCents": 990}""");
		String location = send(post(BASE + "/download-packages"), """
				{"name": "10 Downloads", "kind": "IMAGE_COUNT", "imageCount": 10, "downloadProductId": "%s", "priceCents": 6900}"""
			.formatted(original))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.imageCount").value(10))
			.andReturn()
			.getResponse()
			.getHeader("Location");
		send(post(BASE + "/download-packages"), """
				{"name": "Ganze Galerie", "kind": "WHOLE_GALLERY", "downloadProductId": "%s", "priceCents": 14900}"""
			.formatted(original))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.downloadProductId").value(original))
			.andExpect(jsonPath("$.imageCount").doesNotExist());

		send(post(BASE + "/download-packages"), """
				{"name": "ganze galerie", "kind": "WHOLE_GALLERY", "downloadProductId": "%s", "priceCents": 4900}"""
			.formatted(original))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Ein Paket mit dem Namen „ganze galerie“ gibt es bereits."));
		send(post(BASE + "/download-packages"), """
				{"name": "Ohne Anzahl", "kind": "IMAGE_COUNT", "downloadProductId": "%s", "priceCents": 100}"""
			.formatted(original))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Ein Paket enthält zwischen 2 und 10.000 Bilder."));
		send(post(BASE + "/download-packages"), """
				{"name": "Galerie mit Anzahl", "kind": "WHOLE_GALLERY", "imageCount": 5, "downloadProductId": "%s", "priceCents": 100}"""
			.formatted(original))
			.andExpect(status().isBadRequest());
		send(post(BASE + "/download-packages"), """
				{"name": "Unbekannte Variante", "kind": "WHOLE_GALLERY", "downloadProductId": "%s", "priceCents": 100}"""
			.formatted(java.util.UUID.randomUUID()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Die gewählte Download-Variante gibt es nicht."));
		String print = createProduct("""
				{"type": "PRINT", "paperType": "Matt", "printFormat": "10 × 15 cm", "priceCents": 190}""");
		send(post(BASE + "/download-packages"), """
				{"name": "Abzug als Variante", "kind": "WHOLE_GALLERY", "downloadProductId": "%s", "priceCents": 100}"""
			.formatted(print))
			.andExpect(status().isBadRequest());

		// The variant is in use and cannot be deleted
		mockMvc.perform(delete(BASE + "/products/" + original).with(studioAdmin(studio)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith(
					"Die Download-Variante „Original“ wird vom Paket „10 Downloads“ verwendet")));

		send(put(location), """
				{"name": "20 Downloads", "kind": "IMAGE_COUNT", "imageCount": 20, "downloadProductId": "%s", "priceCents": 11900}"""
			.formatted(original))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("20 Downloads"));
		mockMvc.perform(delete(location).with(studioAdmin(studio))).andExpect(status().isNoContent());
		mockMvc.perform(get(location).with(studioAdmin(studio)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Das Download-Paket wurde nicht gefunden."));
	}

	@Test
	void managesShippingMethods() throws Exception {
		String location = send(post(BASE + "/shipping-methods"), """
				{"name": "Standardversand", "priceCents": 490}""")
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getHeader("Location");

		send(post(BASE + "/shipping-methods"), """
				{"name": "STANDARDVERSAND", "priceCents": 590}""")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Eine Versandart mit dem Namen „STANDARDVERSAND“ gibt es bereits."));
		send(post(BASE + "/shipping-methods"), """
				{"name": "  ", "priceCents": 590}""").andExpect(status().isBadRequest());

		send(put(location), """
				{"name": "Standardversand", "priceCents": 590, "active": false}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(false));
		mockMvc.perform(delete(location).with(studioAdmin(studio))).andExpect(status().isNoContent());
		mockMvc.perform(delete(location).with(studioAdmin(studio)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Die Versandart wurde nicht gefunden."));
	}

	@Test
	void priceListIsSortedForDisplay() throws Exception {
		createProduct("""
				{"type": "DOWNLOAD", "downloadName": "Original", "priceCents": 990}""");
		createProduct("""
				{"type": "PRINT", "paperType": "Matt", "printFormat": "20 × 30 cm", "priceCents": 790}""");
		createProduct("""
				{"type": "PRINT", "paperType": "Matt", "printFormat": "10 × 15 cm", "priceCents": 190}""");
		createProduct("""
				{"type": "DOWNLOAD", "downloadName": "Web 2048 px", "maxEdgePx": 2048, "priceCents": 490}""");
		send(post(BASE + "/shipping-methods"), """
				{"name": "Standardversand", "priceCents": 490}""");
		send(post(BASE + "/shipping-methods"), """
				{"name": "Abholung", "priceCents": 0}""");

		mockMvc.perform(get(BASE).with(studioAdmin(studio)))
			.andExpect(jsonPath("$.products[0].printFormat").value("10 × 15 cm"))
			.andExpect(jsonPath("$.products[1].printFormat").value("20 × 30 cm"))
			.andExpect(jsonPath("$.products[2].downloadName").value("Web 2048 px"))
			.andExpect(jsonPath("$.products[3].downloadName").value("Original"))
			.andExpect(jsonPath("$.shippingMethods[0].name").value("Abholung"));
	}

	@Test
	void customersAreOfferedActiveEntriesOnly() throws Exception {
		createProduct("""
				{"type": "PRINT", "paperType": "Matt", "printFormat": "10 × 15 cm", "priceCents": 190}""");
		createProduct("""
				{"type": "PRINT", "paperType": "Fine Art", "printFormat": "30 × 45 cm", "priceCents": 2490, "active": false}""");
		String original = createProduct("""
				{"type": "DOWNLOAD", "downloadName": "Original", "priceCents": 990}""");
		String inactiveVariant = createProduct("""
				{"type": "DOWNLOAD", "downloadName": "Web", "priceCents": 490, "active": false}""");
		send(post(BASE + "/download-packages"), """
				{"name": "Alt", "kind": "WHOLE_GALLERY", "downloadProductId": "%s", "priceCents": 100, "active": false}"""
			.formatted(original));
		send(post(BASE + "/download-packages"), """
				{"name": "Web-Paket", "kind": "WHOLE_GALLERY", "downloadProductId": "%s", "priceCents": 100}"""
			.formatted(inactiveVariant));
		send(post(BASE + "/download-packages"), """
				{"name": "Ganze Galerie", "kind": "WHOLE_GALLERY", "downloadProductId": "%s", "priceCents": 14900}"""
			.formatted(original));
		send(post(BASE + "/shipping-methods"), """
				{"name": "Standardversand", "priceCents": 490}""");
		send(put(BASE + "/settings"), """
				{"vatRatePercent": 7}""");

		PriceOffer offer = TenantContext.callAs(tenant.id(), () -> priceListManagement.offer());

		assertThat(offer.currency()).isEqualTo("EUR");
		assertThat(offer.vatRatePercent()).isEqualByComparingTo(new BigDecimal("7"));
		assertThat(offer.prints()).extracting(Product::paperType).containsExactly("Matt");
		assertThat(offer.downloads()).extracting(Product::downloadName).containsExactly("Original");
		// inactive package and package of an inactive variant are not offered
		assertThat(offer.downloadPackages()).extracting(DownloadPackage::name).containsExactly("Ganze Galerie");
		assertThat(offer.shippingMethods()).extracting(ShippingMethod::name).containsExactly("Standardversand");
	}

	@Test
	void photographersMayReadButNotChangeThePriceList() throws Exception {
		String product = createProduct("""
				{"type": "DOWNLOAD", "downloadName": "Original", "priceCents": 990}""");

		mockMvc.perform(get(BASE).with(photographer(studio)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.products.length()").value(1));
		mockMvc.perform(get(BASE + "/products/" + product).with(photographer(studio))).andExpect(status().isOk());

		mockMvc.perform(post(BASE + "/products").with(photographer(studio))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"type": "DOWNLOAD", "downloadName": "Web", "priceCents": 490}"""))
			.andExpect(status().isForbidden());
		mockMvc.perform(put(BASE + "/products/" + product).with(photographer(studio))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"type": "DOWNLOAD", "downloadName": "Original", "priceCents": 1}"""))
			.andExpect(status().isForbidden());
		mockMvc.perform(delete(BASE + "/products/" + product).with(photographer(studio)))
			.andExpect(status().isForbidden());
		mockMvc.perform(put(BASE + "/settings").with(photographer(studio))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"vatRatePercent": 0}"""))
			.andExpect(status().isForbidden());
		mockMvc.perform(post(BASE + "/shipping-methods").with(photographer(studio))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Express", "priceCents": 990}"""))
			.andExpect(status().isForbidden());

		mockMvc.perform(get(BASE + "/products/" + product).with(studioAdmin(studio)))
			.andExpect(jsonPath("$.priceCents").value(990));
	}

	@Test
	void requiresLogin() throws Exception {
		mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
		mockMvc.perform(post(BASE + "/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isUnauthorized());
	}

	private ResultActions send(MockHttpServletRequestBuilder request, String json) throws Exception {
		return mockMvc.perform(request.with(studioAdmin(studio)).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private String createProduct(String json) throws Exception {
		String body = send(post(BASE + "/products"), json).andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

}
