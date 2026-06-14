package com.kasaca.parkio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ParkioApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParkioApplication.class, args);
	}

}
