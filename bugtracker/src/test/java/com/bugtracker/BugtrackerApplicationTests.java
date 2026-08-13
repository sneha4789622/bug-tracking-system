package com.bugtracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class BugtrackerApplicationTests {

	/**
	 * Mock the mail sender so Spring does not try to connect
	 * to an SMTP server when loading the context in tests.
	 * Without this, JavaMailSenderAutoConfiguration fails
	 * because no real mail server is configured.
	 */
	@MockBean
	private JavaMailSender javaMailSender;

	@Test
	@DisplayName("Spring context loads without errors")
	void contextLoads() {
		// If the context loads successfully this test passes
	}
}