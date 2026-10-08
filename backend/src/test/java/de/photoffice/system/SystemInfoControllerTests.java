package de.photoffice.system;

import de.photoffice.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SystemInfoControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsNameAndVersion() throws Exception {
		mockMvc.perform(get("/api/system/info"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("photoffice-backend"))
			.andExpect(jsonPath("$.version").isNotEmpty());
	}

}
