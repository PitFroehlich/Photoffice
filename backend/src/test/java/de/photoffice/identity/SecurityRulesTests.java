package de.photoffice.identity;

import static de.photoffice.identity.TestTokens.photographer;
import static de.photoffice.identity.TestTokens.platformAdmin;
import static de.photoffice.identity.TestTokens.studioAdmin;
import static de.photoffice.identity.TestTokens.token;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.photoffice.TestcontainersConfiguration;
import de.photoffice.tenant.InitialStudioAdmin;
import de.photoffice.tenant.TenantManagement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityRulesTests {

	private static final InitialStudioAdmin ADMIN = new InitialStudioAdmin("admin@example.test", null, null);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TenantManagement tenantManagement;

	private String studioA;

	private String studioB;

	@BeforeEach
	void registerStudios() {
		studioA = tenantManagement.register(uniqueSlug(), "Studio A", ADMIN).slug();
		studioB = tenantManagement.register(uniqueSlug(), "Studio B", ADMIN).slug();
	}

	@Test
	void publicEndpointsNeedNoToken() throws Exception {
		mockMvc.perform(get("/api/system/info")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void studioEndpointsRequireAToken() throws Exception {
		mockMvc.perform(get("/api/studio/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void studioUserIsBoundToTheStudioOfTheToken() throws Exception {
		mockMvc.perform(get("/api/studio/me").with(studioAdmin(studioA)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.studio.slug").value(studioA))
			.andExpect(jsonPath("$.roles[0]").value("STUDIO_ADMIN"));

		mockMvc.perform(get("/api/studio/me").with(photographer(studioB)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.studio.slug").value(studioB))
			.andExpect(jsonPath("$.roles[0]").value("PHOTOGRAPHER"));
	}

	@Test
	void userWithoutStudioMembershipIsRejected() throws Exception {
		mockMvc.perform(get("/api/studio/me").with(token("nobody", List.of(), "studio-admin")))
			.andExpect(status().isForbidden());
	}

	@Test
	void userInSeveralStudiosIsRejected() throws Exception {
		mockMvc.perform(get("/api/studio/me").with(token("multi", List.of(studioA, studioB), "studio-admin")))
			.andExpect(status().isForbidden());
	}

	@Test
	void unknownStudioIsRejected() throws Exception {
		mockMvc.perform(get("/api/studio/me").with(studioAdmin("unknown-studio"))).andExpect(status().isForbidden());
	}

	@Test
	void suspendedStudioIsRejected() throws Exception {
		tenantManagement.suspend(tenantManagement.findBySlug(studioA).orElseThrow().id());

		mockMvc.perform(get("/api/studio/me").with(studioAdmin(studioA))).andExpect(status().isForbidden());
	}

	@Test
	void studioMembershipWithoutStudioRoleIsRejected() throws Exception {
		mockMvc.perform(get("/api/studio/me").with(token("customer", List.of(studioA), "default-roles-photoffice")))
			.andExpect(status().isForbidden());
	}

	@Test
	void platformAdminIsNoStudioUser() throws Exception {
		mockMvc.perform(get("/api/studio/me").with(platformAdmin())).andExpect(status().isForbidden());
	}

	@Test
	void studioUsersCannotUsePlatformEndpoints() throws Exception {
		mockMvc.perform(get("/api/platform/tenants").with(studioAdmin(studioA))).andExpect(status().isForbidden());
	}

	@Test
	void everythingElseIsDenied() throws Exception {
		mockMvc.perform(get("/api/not-configured").with(studioAdmin(studioA))).andExpect(status().isForbidden());
		mockMvc.perform(get("/actuator/modulith").with(platformAdmin())).andExpect(status().isForbidden());
	}

	private static String uniqueSlug() {
		return "studio-" + UUID.randomUUID().toString().substring(0, 8);
	}

}
