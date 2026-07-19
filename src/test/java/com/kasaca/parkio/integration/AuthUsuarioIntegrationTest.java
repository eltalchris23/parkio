package com.kasaca.parkio.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.usuario.dto.UsuarioCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integracion para validar el flujo real de usuario y autenticacion.
 *
 * <p>Estas pruebas levantan Spring Boot completo, ejecutan Flyway contra
 * PostgreSQL y consumen endpoints HTTP reales usando un puerto aleatorio.</p>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthUsuarioIntegrationTest {

    private static final String TEST_DATABASE_NAME = "parkio_test";
    private static final String EMAIL = "integration.user@parkio.com";
    private static final String PASSWORD = "clave-integracion";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Limpia datos generados por pruebas anteriores para que cada test sea
     * independiente y repetible.
     *
     * <p>Antes de limpiar valida que la conexion apunte a parkio_test. Esta
     * proteccion evita borrar datos si por error se ejecutan las pruebas contra
     * la base local de desarrollo.</p>
     */
    @BeforeEach
    void limpiarDatosDePrueba() {
        validarBaseDeDatosDePrueba();

        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    usuario_rol,
                    usuario_estacionamiento,
                    cajon,
                    estacionamiento,
                    usuario
                RESTART IDENTITY
                CASCADE
                """);
    }

    /**
     * Verifica que el perfil test use la base parkio_test y que Flyway haya
     * insertado los roles base necesarios para el funcionamiento del sistema.
     */
    @Test
    void debeUsarBaseDeDatosDePruebaYTenerRolesBase() {
        validarBaseDeDatosDePrueba();

        Integer rolesBase = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM rol
                WHERE nombre IN ('ADMIN', 'OWNER', 'OPERADOR', 'USER')
                """,
                Integer.class
        );

        assertThat(rolesBase).isEqualTo(4);
    }

    /**
     * Verifica que un endpoint protegido rechace solicitudes sin JWT.
     */
    @Test
    void debeRechazarConsultaDeUsuariosSinToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/usuarios",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Verifica que /auth/me rechace solicitudes sin JWT porque es un endpoint protegido.
     */
    @Test
    void debeRechazarConsultaDeUsuarioAutenticadoSinToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/auth/me",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Valida el flujo completo de registro publico, login con BCrypt/JWT y
     * consulta protegida del propio usuario usando el token emitido.
     */
    @Test
    void debeRegistrarUsuarioIniciarSesionYConsultarUsuarioPropioConJwt() throws Exception {
        ResponseEntity<String> createResponse = registrarUsuario();
        JsonNode createBody = objectMapper.readTree(createResponse.getBody());
        Long usuarioId = createBody.path("data").path("id").asLong();

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createBody.path("status").asInt()).isEqualTo(201);
        assertThat(createBody.path("transactionId").asText()).isNotBlank();
        assertThat(createBody.path("data").path("email").asText()).isEqualTo(EMAIL);
        assertThat(createBody.path("data").path("roles").get(0).asText()).isEqualTo("USER");

        ResponseEntity<String> loginResponse = iniciarSesion();
        JsonNode loginBody = objectMapper.readTree(loginResponse.getBody());
        String accessToken = loginBody.path("accessToken").asText();

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(accessToken).isNotBlank();
        assertThat(loginBody.path("tokenType").asText()).isEqualTo("Bearer");

        ResponseEntity<String> userResponse = consultarUsuarioPropio(usuarioId, accessToken);
        JsonNode userBody = objectMapper.readTree(userResponse.getBody());

        assertThat(userResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userBody.path("status").asInt()).isEqualTo(200);
        assertThat(userBody.path("transactionId").asText()).isNotBlank();
        assertThat(userBody.path("data").path("id").asLong()).isEqualTo(usuarioId);
        assertThat(userBody.path("data").path("email").asText()).isEqualTo(EMAIL);
        assertThat(userBody.path("data").path("passwordHash").isMissingNode()).isTrue();
    }

    /**
     * Valida el flujo completo de registro publico, login y consulta del usuario
     * autenticado mediante /auth/me usando el JWT real emitido por el backend.
     */
    @Test
    void debeConsultarUsuarioAutenticadoConJwt() throws Exception {
        ResponseEntity<String> createResponse = registrarUsuario();
        JsonNode createBody = objectMapper.readTree(createResponse.getBody());
        Long usuarioId = createBody.path("data").path("id").asLong();

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> loginResponse = iniciarSesion();
        JsonNode loginBody = objectMapper.readTree(loginResponse.getBody());
        String accessToken = loginBody.path("accessToken").asText();

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(accessToken).isNotBlank();

        ResponseEntity<String> meResponse = consultarUsuarioAutenticado(accessToken);
        JsonNode meBody = objectMapper.readTree(meResponse.getBody());

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meBody.path("status").asInt()).isEqualTo(200);
        assertThat(meBody.path("message").asText()).isEqualTo("Usuario autenticado consultado correctamente");
        assertThat(meBody.path("transactionId").asText()).isNotBlank();
        assertThat(meBody.path("data").path("id").asLong()).isEqualTo(usuarioId);
        assertThat(meBody.path("data").path("email").asText()).isEqualTo(EMAIL);
        assertThat(meBody.path("data").path("roles").get(0).asText()).isEqualTo("USER");
        assertThat(meBody.path("data").path("passwordHash").isMissingNode()).isTrue();
    }

    /**
     * Crea un usuario real mediante el endpoint publico de registro.
     */
    private ResponseEntity<String> registrarUsuario() {
        UsuarioCreateRequest request = new UsuarioCreateRequest(
                "Usuario",
                "Integracion",
                EMAIL,
                PASSWORD
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate.postForEntity(
                "/api/v1/usuarios",
                new HttpEntity<>(request, headers),
                String.class
        );
    }

    /**
     * Inicia sesion con el usuario creado y devuelve la respuesta HTTP real.
     */
    private ResponseEntity<String> iniciarSesion() {
        AuthLoginRequest request = new AuthLoginRequest(EMAIL, PASSWORD);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(request, headers),
                String.class
        );
    }

    /**
     * Consulta el usuario autenticado agregando el JWT en Authorization.
     */
    private ResponseEntity<String> consultarUsuarioPropio(Long usuarioId, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        return restTemplate.exchange(
                "/api/v1/usuarios/" + usuarioId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
    }

    /**
     * Consulta el usuario autenticado actual agregando el JWT en Authorization.
     */
    private ResponseEntity<String> consultarUsuarioAutenticado(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        return restTemplate.exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
    }

    /**
     * Confirma que la conexion activa corresponde a la base segura de pruebas.
     */
    private void validarBaseDeDatosDePrueba() {
        String databaseName = jdbcTemplate.queryForObject(
                "SELECT current_database()",
                String.class
        );

        assertThat(databaseName).isEqualTo(TEST_DATABASE_NAME);
    }
}

