package com.kasaca.parkio.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion para validar que los endpoints de health check
 * puedan ser consultados sin JWT.
 *
 * <p>Estos endpoints son consumidos normalmente por herramientas externas
 * de monitoreo, contenedores, balanceadores o plataformas de despliegue.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthCheckSecurityIntegrationTest {

    private final MockMvc mockMvc;

    /**
     * Recibe MockMvc por constructor para ejecutar peticiones HTTP simuladas
     * contra el contexto completo de Spring Boot.
     *
     * @param mockMvc cliente de pruebas para llamar endpoints sin levantar un servidor real
     */
    @Autowired
    HealthCheckSecurityIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /**
     * Verifica que el health check general sea publico.
     *
     * <p>No debe requerir JWT porque su objetivo es permitir que un monitor
     * externo confirme si Parkio esta disponible.</p>
     */
    @Test
    void debePermitirHealthCheckSinToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                // Confirma que Spring Security permite consultar el endpoint sin Authorization.
                .andExpect(status().isOk())
                // Confirma que Actuator responde con el estado general de la aplicacion.
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /**
     * Verifica que el endpoint de liveness sea publico.
     *
     * <p>Liveness indica si el proceso de la aplicacion sigue vivo.</p>
     */
    @Test
    void debePermitirLivenessSinToken() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                // Confirma que la verificacion de vida puede consultarse sin JWT.
                .andExpect(status().isOk())
                // Confirma que Actuator reporta el estado del proceso.
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /**
     * Verifica que el endpoint de readiness sea publico.
     *
     * <p>Readiness indica si la aplicacion esta lista para recibir trafico.</p>
     */
    @Test
    void debePermitirReadinessSinToken() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                // Confirma que la verificacion de disponibilidad puede consultarse sin JWT.
                .andExpect(status().isOk())
                // Confirma que Actuator reporta que la aplicacion esta lista.
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
