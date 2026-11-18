package com.edu.unimag.rodatours;

import com.edu.unimag.rodatours.repositories.TestPostgresContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class RodatoursApplicationTests {

	@DynamicPropertySource
	static void postgres(DynamicPropertyRegistry registry) {
		TestPostgresContainer.register(registry);
	}

	@Test
	void contextLoads() {
	}

}
