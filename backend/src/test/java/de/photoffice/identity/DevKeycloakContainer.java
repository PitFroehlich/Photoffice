package de.photoffice.identity;

import java.nio.file.Path;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Keycloak container as in {@code docker-compose.yml}: dev realm ({@code infra/keycloak/photoffice-realm.json}) and
 * Photoffice theme ({@code infra/keycloak/themes/photoffice}, issue #36). Bootstrap admin: admin / admin.
 */
final class DevKeycloakContainer {

	static final String IMAGE = "quay.io/keycloak/keycloak:26.8";

	private DevKeycloakContainer() {
	}

	static GenericContainer<?> create() {
		return new GenericContainer<>(IMAGE).withCommand("start-dev", "--import-realm")
			.withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
			.withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
			.withCopyFileToContainer(MountableFile.forHostPath(infra("keycloak/photoffice-realm.json")),
					"/opt/keycloak/data/import/photoffice-realm.json")
			.withCopyFileToContainer(MountableFile.forHostPath(infra("keycloak/themes/photoffice")),
					"/opt/keycloak/themes/photoffice")
			.withExposedPorts(8080)
			.waitingFor(Wait.forHttp("/realms/photoffice").forPort(8080).forStatusCode(200))
			// Realm import takes longer on a busy machine (parallel builds) than the default 60 s
			.withStartupTimeout(Duration.ofMinutes(3));
	}

	private static Path infra(String path) {
		return Path.of("../infra").resolve(path).toAbsolutePath();
	}

}
