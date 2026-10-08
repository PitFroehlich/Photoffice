package de.photoffice;

import org.springframework.boot.SpringApplication;

public class TestPhotofficeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(PhotofficeBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
