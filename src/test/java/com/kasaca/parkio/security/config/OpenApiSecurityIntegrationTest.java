package com.kasaca.parkio.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion para validar que OpenAPI y Swagger UI sean accesibles
 * sin JWT cuando Springdoc esta habilitado por configuracion.
 *
 * <p>El perfil test mantiene Springdoc deshabilitado por defecto, por eso esta
 * clase lo activa explicitamente solo para validar la regla de seguridad.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true",
        "springdoc.swagger-ui.path=/swagger-ui.html"
})
class OpenApiSecurityIntegrationTest {

    private final MockMvc mockMvc;

    /**
     * Recibe MockMvc por constructor para ejecutar peticiones HTTP simuladas
     * contra el contexto completo de Spring Boot.
     *
     * @param mockMvc cliente de pruebas para validar endpoints web sin levantar un servidor real
     */
    @Autowired
    OpenApiSecurityIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /**
     * Verifica que el contrato OpenAPI JSON sea publico cuando Springdoc esta habilitado.
     *
     * <p>Este endpoint es consumido por Swagger UI para construir la documentacion
     * interactiva de los controllers reales de Parkio.</p>
     */
    @Test
    void debePermitirOpenApiDocsSinTokenCuandoSpringdocEstaHabilitado() throws Exception {
        mockMvc.perform(get("/api/v1/v3/api-docs")
                        // Simula el prefijo global spring.mvc.servlet.path=/api/v1 usado por la app real.
                        .servletPath("/api/v1"))
                // Confirma que Spring Security permite consultar el contrato sin JWT.
                .andExpect(status().isOk())
                // Confirma que el documento generado corresponde a la API de Parkio.
                .andExpect(jsonPath("$.info.title").value("Parkio API"));
    }

    /**
     * Verifica que Swagger UI sea publico cuando Springdoc esta habilitado.
     *
     * <p>Swagger UI se usa en desarrollo para probar endpoints desde navegador.
     * En produccion permanece deshabilitado mediante configuracion.</p>
     */
    @Test
    void debePermitirSwaggerUiSinTokenCuandoSpringdocEstaHabilitado() throws Exception {
        mockMvc.perform(get("/api/v1/swagger-ui.html")
                        // Simula el prefijo global spring.mvc.servlet.path=/api/v1 usado por la app real.
                        .servletPath("/api/v1"))
                // Springdoc normalmente redirige /api/v1/swagger-ui.html hacia /api/v1/swagger-ui/index.html.
                .andExpect(status().is3xxRedirection());
    }
}
