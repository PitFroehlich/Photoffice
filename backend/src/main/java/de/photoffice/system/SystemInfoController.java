package de.photoffice.system;

import de.photoffice.api.SystemApi;
import de.photoffice.api.model.SystemInfo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class SystemInfoController implements SystemApi {

	private final BuildProperties buildProperties;

	SystemInfoController(ObjectProvider<BuildProperties> buildProperties) {
		this.buildProperties = buildProperties.getIfAvailable();
	}

	@Override
	public ResponseEntity<SystemInfo> getSystemInfo() {
		if (buildProperties == null) {
			return ResponseEntity.ok(new SystemInfo("photoffice-backend", "dev"));
		}
		return ResponseEntity.ok(new SystemInfo(buildProperties.getArtifact(), buildProperties.getVersion()));
	}

}
