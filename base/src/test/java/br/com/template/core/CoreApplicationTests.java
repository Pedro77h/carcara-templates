package br.com.template.core;

import br.com.template.core.support.CoreTestServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CoreApplicationTests extends CoreTestServer {

	@Autowired
	private Environment environment;

	@Test
	void contextLoads() {
	}

	@Test
	@DisplayName("should load the main application.properties under the test overrides, never the local profile")
	void shouldLoadMainPropertiesUnderTestOverrides() {
		// Só existe no principal: se sumir, o arquivo de teste voltou a substituí-lo (ACH-20261009-001).
		assertThat(environment.getProperty("app.auth.rate-limit.max-attempts-per-origin")).isEqualTo("20");
		assertThat(environment.getActiveProfiles()).containsExactly("test");
		assertThat(environment.getProperty("spring.flyway.locations")).doesNotContain("migration-dev");
	}

}
