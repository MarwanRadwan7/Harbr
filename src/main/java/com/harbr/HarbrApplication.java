package com.harbr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(excludeName = "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration")
@ConfigurationPropertiesScan
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.harbr")
public class HarbrApplication {

	public static void main(String[] args) {
		SpringApplication.run(HarbrApplication.class, args);
	}

}