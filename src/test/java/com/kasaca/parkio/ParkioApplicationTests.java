package com.kasaca.parkio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class ParkioApplicationTests {

	/**
	 * Verifica que el contexto completo de Spring Boot pueda iniciar correctamente.
	 *
	 * <p>La prueba usa el perfil {@code test} para evitar que Flyway, JPA o cualquier
	 * bean de infraestructura apunte accidentalmente a la base local de desarrollo
	 * {@code parkio}. De esta forma, las migraciones y validaciones de esquema se
	 * ejecutan sobre {@code parkio_test}.
	 */
	@Test
	void contextLoads() {
	}

}
