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
		tenant = tenantManagement.register("studio-" + UUID.randomUUID().toString().substring(0, 8), "Studio");
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
			.andExpect(jsonPath("$.resolution").doesNotExist())
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
	void createsDownloadsPerResolution() throws Exception {
		createProduct("""
				{"type": "DOWNLOAD", "resolution": "WEB", "priceCents": 490}""");
		createProduct("""
				{"type": "DOWNLOAD", "resolution": "FULL", "priceCents": 990}""");

		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "resolution": "FULL", "priceCents": 1290}""")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Den Download in voller Auflösung gibt es bereits."));
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
				{"type": "PRINT", "paperType": "Matt", "printFormat": "10 × 15 cm", "resolution": "WEB", "priceCents": 290}""")
			.andExpect(status().isBadRequest());
		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "priceCents": 290}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Für einen Download ist die Auflösung Pflicht."));
		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "resolution": "WEB", "paperType": "Matt", "priceCents": 290}""")
			.andExpect(status().isBadRequest());
		send(post(BASE + "/products"), """
				{"type": "BOOK", "priceCents": 290}""").andExpect(status().isBadRequest());

		String print = createProduct("""
				{"type": "PRINT", "paperType": "Matt", "printFormat": "10 × 15 cm", "priceCents": 190}""");
		send(put(BASE + "/products/" + print), """
				{"type": "DOWNLOAD", "resolution": "WEB", "priceCents": 190}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Der Produkttyp kann nicht geändert werden."));
	}

	@Test
	void pricesAreWholeCentsWithinLimits() throws Exception {
		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "resolution": "WEB", "priceCents": -1}""").andExpect(status().isBadRequest());
		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "resolution": "WEB", "priceCents": 10000001}""").andExpect(status().isBadRequest());
		send(post(BASE + "/products"), """
				{"type": "DOWNLOAD", "resolution": "WEB", "priceCents": 4.9}""").andExpect(status().isBadRequest());
		send(post(BASE + "/shipping-methods"), """
				{"name": "Abholung im Studio", "priceCents": 0}""").andExpect(status().isCreated());
	}

	@Test
	void managesDownloadPackages() throws Exception {
		String location = send(post(BASE + "/download-packages"), """
				{"name": "10 Downloads", "kind": "IMAGE_COUNT", "imageCount": 10, "resolution": "FULL", "priceCents": 6900}""")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.imageCount").value(10))
			.andReturn()
			.getResponse()
			.getHeader("Location");
		send(post(BASE + "/download-packages"), """
				{"name": "Ganze Galerie", "kind": "WHOLE_GALLERY", "resolution": "FULL", "priceCents": 14900}""")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.imageCount").doesNotExist());

		send(post(BASE + "/download-packages"), """
				{"name": "ganze galerie", "kind": "WHOLE_GALLERY", "resolution": "WEB", "priceCents": 4900}""")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Ein Paket mit dem Namen „ganze galerie“ gibt es bereits."));
		send(post(BASE + "/download-packages"), """
				{"name": "Ohne Anzahl", "kind": "IMAGE_COUNT", "resolution": "FULL", "priceCents": 100}""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Ein Paket enthält zwischen 2 und 10.000 Bilder."));
		send(post(BASE + "/download-packages"), """
				{"name": "Galerie mit Anzahl", "kind": "WHOLE_GALLERY", "imageCount": 5, "resolution": "FULL", "priceCents": 100}""")
			.andExpect(status().isBadRequest());

		send(put(location), """
				{"name": "20 Downloads", "kind": "IMAGE_COUNT", "imageCount": 20, "resolution": "FULL", "priceCents": 11900}""")
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
				{"type": "DOWNLOAD", "resolution": "FULL", "priceCents": 990}""");
		createProduct("""
				{"type": "PRINT", "paperType": "Matt", "printFormat": "20 × 30 cm", "priceCents": 790}""");
		createProduct("""
				{"type": "PRINT", "paperType": "Matt", "printFormat": "10 × 15 cm", "priceCents": 190}""");
		createProduct("""
				{"type": "DOWNLOAD", "resolution": "WEB", "priceCents": 490}""");
		send(post(BASE + "/shipping-methods"), """
				{"name": "Standardversand", "priceCents": 490}""");
		send(post(BASE + "/shipping-methods"), """
				{"name": "Abholung", "priceCents": 0}""");

		mockMvc.perform(get(BASE).with(studioAdmin(studio)))
			.andExpect(jsonPath("$.products[0].printFormat").value("10 × 15 cm"))
			.andExpect(jsonPath("$.products[1].printFormat").value("20 × 30 cm"))
			.andExpect(jsonPath("$.products[2].resolution").value("WEB"))
			.andExpect(jsonPath("$.products[3].resolution").value("FULL"))
			.andExpect(jsonPath("$.shippingMethods[0].name").value("Abholung"));
	}

	@Test
	void customersAreOfferedActiveEntriesOnly() throws Exception {
		createProduct("""
				{"type": "PRINT", "paperType": "Matt", "printFormat": "10 × 15 cm", "priceCents": 190}""");
		createProduct("""
				{"type": "PRINT", "paperType": "Fine Art", "printFormat": "30 × 45 cm", "priceCents": 2490, "active": false}""");
		createProduct("""
				{"type": "DOWNLOAD", "resolution": "FULL", "priceCents": 990}""");
		send(post(BASE + "/download-packages"), """
				{"name": "Alt", "kind": "WHOLE_GALLERY", "resolution": "FULL", "priceCents": 100, "active": false}""");
		send(post(BASE + "/shipping-methods"), """
				{"name": "Standardversand", "priceCents": 490}""");
		send(put(BASE + "/settings"), """
				{"vatRatePercent": 7}""");

		PriceOffer offer = TenantContext.callAs(tenant.id(), () -> priceListManagement.offer());

		assertThat(offer.currency()).isEqualTo("EUR");
		assertThat(offer.vatRatePercent()).isEqualByComparingTo(new BigDecimal("7"));
		assertThat(offer.prints()).extracting(Product::paperType).containsExactly("Matt");
		assertThat(offer.downloads()).extracting(Product::resolution).containsExactly(DownloadResolution.FULL);
		assertThat(offer.downloadPackages()).isEmpty();
		assertThat(offer.shippingMethods()).extracting(ShippingMethod::name).containsExactly("Standardversand");
	}

	@Test
	void photographersMayReadButNotChangeThePriceList() throws Exception {
		String product = createProduct("""
				{"type": "DOWNLOAD", "resolution": "FULL", "priceCents": 990}""");

		mockMvc.perform(get(BASE).with(photographer(studio)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.products.length()").value(1));
		mockMvc.perform(get(BASE + "/products/" + product).with(photographer(studio))).andExpect(status().isOk());

		mockMvc.perform(post(BASE + "/products").with(photographer(studio))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"type": "DOWNLOAD", "resolution": "WEB", "priceCents": 490}"""))
			.andExpect(status().isForbidden());
		mockMvc.perform(put(BASE + "/products/" + product).with(photographer(studio))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"type": "DOWNLOAD", "resolution": "FULL", "priceCents": 1}"""))
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
