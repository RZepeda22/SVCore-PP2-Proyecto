package com.svcore.api_demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** La aplicación arranca contra MySQL real: Flyway aplica las migraciones y Hibernate valida el esquema. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ApiDemoApplicationTests {

	@Test
	void contextLoads() {
	}
}
