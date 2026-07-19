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
 * Pruebas de integracion para validar el modulo Catalogos usando la aplicacion completa.
 *
 * <p>Estas pruebas levantan Spring Boot, ejecutan Flyway contra PostgreSQL,
 * consumen endpoints HTTP reales y validan que los catalogos de cajones esten
 * protegidos por JWT y disponibles para ADMIN, OPERADOR y USER.</p>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogoIntegrationTest {

    private static final String TEST_DATABASE_NAME = "parkio_test";
    private static final String ADMIN_EMAIL = "integration.admin.catalogo@parkio.com";
    private static final String OPERADOR_EMAIL = "integration.operador.catalogo@parkio.com";
    private static final String USER_EMAIL = "integration.user.catalogo@parkio.com";
    private static final String PASSWORD = "clave-integracion";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Limpia los datos variables de la prueba y conserva los roles base creados por Flyway.
     *
     * <p>Antes de limpiar valida que la conexion apunte a parkio_test. Esta proteccion
     * evita borrar informacion real si el perfil de pruebas se configura de forma incorrecta.</p>
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

        jdbcTemplate.update("""
                UPDATE rol
                SET activo = TRUE
                WHERE nombre IN ('ADMIN', 'OWNER', 'OPERADOR', 'USER')
                """);
    }

    /**
     * Verifica que los catalogos de cajones rechacen solicitudes sin JWT.
     */
    @Test
    void debeRechazarCatalogosSinToken() {
        ResponseEntity<String> tiposResponse = restTemplate.getForEntity(
                "/api/v1/catalogos/cajones/tipos",
                String.class
        );

        ResponseEntity<String> estadosResponse = restTemplate.getForEntity(
                "/api/v1/catalogos/cajones/estados",
                String.class
        );

        assertThat(tiposResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(estadosResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Verifica que un usuario con rol ADMIN pueda consultar los catalogos de cajones.
     */
    @Test
    void debeConsultarCatalogosConRolAdmin() throws Exception {
        Long adminId = registrarUsuario("Administrador", ADMIN_EMAIL);
        asignarRol(adminId, "ADMIN");
        String accessToken = iniciarSesion(ADMIN_EMAIL);

        validarCatalogosDisponibles(accessToken);
    }

    /**
     * Verifica que un usuario con rol OPERADOR pueda consultar los catalogos de cajones.
     */
    @Test
    void debeConsultarCatalogosConRolOperador() throws Exception {
        Long operadorId = registrarUsuario("Operador", OPERADOR_EMAIL);
        asignarRol(operadorId, "OPERADOR");
        String accessToken = iniciarSesion(OPERADOR_EMAIL);

        validarCatalogosDisponibles(accessToken);
    }

    /**
     * Verifica que un usuario con rol USER pueda consultar los catalogos de cajones.
     */
    @Test
    void debeConsultarCatalogosConRolUser() throws Exception {
        registrarUsuario("Usuario", USER_EMAIL);
        String accessToken = iniciarSesion(USER_EMAIL);

        validarCatalogosDisponibles(accessToken);
    }

    /**
     * Valida que ambos catalogos respondan correctamente con el formato ApiResponse.
     *
     * @param accessToken JWT valido emitido por el backend
     */
    private void validarCatalogosDisponibles(String accessToken) throws Exception {
        ResponseEntity<String> tiposResponse = consultarTiposCajon(accessToken);
        ResponseEntity<String> estadosResponse = consultarEstadosCajon(accessToken);

        JsonNode tiposBody = objectMapper.readTree(tiposResponse.getBody());
        JsonNode estadosBody = objectMapper.readTree(estadosResponse.getBody());

        validarRespuestaTipos(tiposResponse, tiposBody);
        validarRespuestaEstados(estadosResponse, estadosBody);
    }

    /**
     * Valida la respuesta del catalogo de tipos de cajon.
     *
     * @param response respuesta HTTP recibida
     * @param body cuerpo JSON parseado
     */
    private void validarRespuestaTipos(ResponseEntity<String> response, JsonNode body) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.path("status").asInt()).isEqualTo(200);
        assertThat(body.path("message").asText()).isEqualTo("Tipos de cajon consultados correctamente");
        assertThat(body.path("transactionId").asText()).isNotBlank();

        assertThat(body.path("data").isArray()).isTrue();
        assertThat(body.path("data").size()).isEqualTo(4);

        assertThat(body.path("data").get(0).path("codigo").asText()).isEqualTo("AUTO");
        assertThat(body.path("data").get(0).path("descripcion").asText()).isEqualTo("Auto");

        assertThat(body.path("data").get(1).path("codigo").asText()).isEqualTo("MOTO");
        assertThat(body.path("data").get(1).path("descripcion").asText()).isEqualTo("Moto");

        assertThat(body.path("data").get(2).path("codigo").asText()).isEqualTo("DISCAPACITADO");
        assertThat(body.path("data").get(2).path("descripcion").asText()).isEqualTo("Discapacitado");

        assertThat(body.path("data").get(3).path("codigo").asText()).isEqualTo("ELECTRICO");
        assertThat(body.path("data").get(3).path("descripcion").asText()).isEqualTo("Electrico");
    }

    /**
     * Valida la respuesta del catalogo de estados de cajon.
     *
     * @param response respuesta HTTP recibida
     * @param body cuerpo JSON parseado
     */
    private void validarRespuestaEstados(ResponseEntity<String> response, JsonNode body) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.path("status").asInt()).isEqualTo(200);
        assertThat(body.path("message").asText()).isEqualTo("Estados de cajon consultados correctamente");
        assertThat(body.path("transactionId").asText()).isNotBlank();

        assertThat(body.path("data").isArray()).isTrue();
        assertThat(body.path("data").size()).isEqualTo(3);

        assertThat(body.path("data").get(0).path("codigo").asText()).isEqualTo("LIBRE");
        assertThat(body.path("data").get(0).path("descripcion").asText()).isEqualTo("Libre");

        assertThat(body.path("data").get(1).path("codigo").asText()).isEqualTo("OCUPADO");
        assertThat(body.path("data").get(1).path("descripcion").asText()).isEqualTo("Ocupado");

        assertThat(body.path("data").get(2).path("codigo").asText()).isEqualTo("FUERA_SERVICIO");
        assertThat(body.path("data").get(2).path("descripcion").asText()).isEqualTo("Fuera de servicio");
    }

    /**
     * Crea un usuario mediante el endpoint publico y devuelve su identificador.
     *
     * @param nombre nombre del usuario de prueba
     * @param email correo unico del usuario de prueba
     * @return identificador del usuario creado
     */
    private Long registrarUsuario(String nombre, String email) throws Exception {
        UsuarioCreateRequest request = new UsuarioCreateRequest(
                nombre,
                "Integracion",
                email,
                PASSWORD
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/usuarios",
                new HttpEntity<>(request, headers),
                String.class
        );

        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return body.path("data").path("id").asLong();
    }

    /**
     * Asigna el rol indicado directamente en la tabla intermedia para simular bootstrap controlado.
     *
     * @param usuarioId identificador del usuario al que se asignara el rol
     * @param rolNombre nombre tecnico del rol que se asignara
     */
    private void asignarRol(Long usuarioId, String rolNombre) {
        jdbcTemplate.update("""
                INSERT INTO usuario_rol (usuario_id, rol_id)
                SELECT ?, rol.id
                FROM rol
                WHERE rol.nombre = ?
                ON CONFLICT DO NOTHING
                """,
                usuarioId,
                rolNombre
        );
    }

    /**
     * Inicia sesion con un usuario existente y devuelve el JWT emitido por el backend.
     *
     * @param email correo del usuario que iniciara sesion
     * @return JWT emitido por el backend
     */
    private String iniciarSesion(String email) throws Exception {
        AuthLoginRequest request = new AuthLoginRequest(email, PASSWORD);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(request, headers),
                String.class
        );

        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return body.path("accessToken").asText();
    }

    /**
     * Consulta el catalogo de tipos de cajon usando autenticacion Bearer.
     *
     * @param accessToken JWT valido
     * @return respuesta HTTP del endpoint
     */
    private ResponseEntity<String> consultarTiposCajon(String accessToken) {
        return restTemplate.exchange(
                "/api/v1/catalogos/cajones/tipos",
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Consulta el catalogo de estados de cajon usando autenticacion Bearer.
     *
     * @param accessToken JWT valido
     * @return respuesta HTTP del endpoint
     */
    private ResponseEntity<String> consultarEstadosCajon(String accessToken) {
        return restTemplate.exchange(
                "/api/v1/catalogos/cajones/estados",
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Crea headers HTTP con Content-Type JSON y el JWT en formato Bearer.
     *
     * @param accessToken JWT emitido por el backend
     * @return headers listos para consumir endpoints protegidos
     */
    private HttpHeaders crearHeadersConJwt(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
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
