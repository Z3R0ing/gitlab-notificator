package ru.z3r0ing.gitlabnotificator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class GitlabNotificatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(GitlabNotificatorApplication.class, args);
	}

}
