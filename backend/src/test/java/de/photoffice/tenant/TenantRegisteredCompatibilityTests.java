package de.photoffice.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import de.photoffice.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.events.core.EventSerializer;

/**
 * Publications of {@link TenantRegistered} from before issue #42 still contain the first studio admin and may still
 * be pending in {@code event_publication} (failed onboarding) – they must remain readable (ADR 0012).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TenantRegisteredCompatibilityTests {

	@Autowired
	private EventSerializer serializer;

	@Test
	void readsPublicationsWithTheFirstStudioAdmin() {
		UUID id = UUID.randomUUID();
		String before42 = """
				{"tenantId":{"value":"%s"},"slug":"konflikt-studio","name":"Konflikt-Studio",\
				"admin":{"email":"admin@studio-a.test","firstName":null,"lastName":"Alt"}}""".formatted(id);

		Object event = serializer.deserialize(before42, TenantRegistered.class);

		assertThat(event).isEqualTo(new TenantRegistered(TenantId.of(id), "konflikt-studio", "Konflikt-Studio",
				new InitialStudioAdmin("admin@studio-a.test", null, "Alt")));
	}

	@Test
	void newPublicationsCarryNoPersonalData() {
		var event = new TenantRegistered(TenantId.of(UUID.randomUUID()), "neues-studio", "Neues Studio");

		String serialized = serializer.serialize(event).toString();

		assertThat(serialized).doesNotContain("@");
		assertThat(serializer.deserialize(serialized, TenantRegistered.class)).isEqualTo(event);
	}

}
