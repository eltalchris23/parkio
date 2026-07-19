package com.kasaca.parkio.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.rol.dto.RolRequest;
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
 * Pruebas de integracion para validar el modulo Rol usando la aplicacion completa.
 *
 * <p>Estas pruebas levantan Spring Boot, ejecutan Flyway contra PostgreSQL,
 * consumen endpoints HTTP reales y validan que la seguridad por JWT y rol ADMIN
 * funcione junto con el CRUD de roles.</p>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RolIntegrationTest {

    private static final String TEST_DATABASE_NAME = "parkio_test";
    private static final String ADMIN_EMAIL = "integration.admin.rol@parkio.com";
    private static final String USER_EMAIL = "integration.user.rol@parkio.com";
    private static final String PASSWORD = "clave-integracion";
    private static final String ROL_NOMBRE = "SUPERVISOR_INTEGRACION";
    private static final String ROL_NOMBRE_ACTUALIZADO = "SUPERVISOR_GENERAL_INTEGRACION";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Limpia los datos variables de la prueba sin eliminar los roles base creados por Flyway.
     *
     * <p>Antes de limpiar valida que la conexion apunte a parkio_test. Esta proteccion
     * evita modificar informacion real si el perfil de pruebas se configura mal.</p>
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
                DELETE FROM rol
                WHERE nombre NOT IN ('ADMIN', 'OWNER', 'OPERADOR', 'USER')
                """);

        jdbcTemplate.update("""
                UPDATE rol
                SET activo = TRUE
                WHERE nombre IN ('ADMIN', 'OWNER', 'OPERADOR', 'USER')
                """);
    }

    /**
     * Verifica que el endpoint protegido de roles rechace solicitudes sin JWT.
     */
    @Test
    void debeRechazarConsultaDeRolesSinToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/roles",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Verifica que un usuario autenticado sin rol ADMIN no pueda consultar roles.
     */
    @Test
    void debeRechazarConsultaDeRolesConUsuarioSinRolAdmin() throws Exception {
        registrarUsuario("Usuario", USER_EMAIL);
        String accessToken = iniciarSesion(USER_EMAIL);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/roles",
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Valida el flujo completo del CRUD de roles usando un JWT con rol ADMIN.
     */
    @Test
    void debeAdministrarRolesConJwtAdmin() throws Exception {
        Long adminId = registrarUsuario("Administrador", ADMIN_EMAIL);
        asignarRolAdmin(adminId);
        String accessToken = iniciarSesion(ADMIN_EMAIL);

        ResponseEntity<String> listadoResponse = listarRoles(accessToken);
        JsonNode listadoBody = objectMapper.readTree(listadoResponse.getBody());

        assertThat(listadoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listadoBody.path("status").asInt()).isEqualTo(200);
        assertThat(listadoBody.path("transactionId").asText()).isNotBlank();
        assertThat(listadoBody.path("data").path("content").size()).isGreaterThanOrEqualTo(3);

        ResponseEntity<String> crearResponse = crearRol(accessToken, ROL_NOMBRE);
        JsonNode crearBody = objectMapper.readTree(crearResponse.getBody());
        Long rolId = crearBody.path("data").path("id").asLong();

        assertThat(crearResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(crearBody.path("status").asInt()).isEqualTo(201);
        assertThat(crearBody.path("transactionId").asText()).isNotBlank();
        assertThat(crearBody.path("data").path("nombre").asText()).isEqualTo(ROL_NOMBRE);
        assertThat(crearBody.path("data").path("activo").asBoolean()).isTrue();

        ResponseEntity<String> consultarResponse = consultarRol(accessToken, rolId);
        JsonNode consultarBody = objectMapper.readTree(consultarResponse.getBody());

        assertThat(consultarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultarBody.path("data").path("id").asLong()).isEqualTo(rolId);
        assertThat(consultarBody.path("data").path("nombre").asText()).isEqualTo(ROL_NOMBRE);

        ResponseEntity<String> actualizarResponse = actualizarRol(accessToken, rolId, ROL_NOMBRE_ACTUALIZADO);
        JsonNode actualizarBody = objectMapper.readTree(actualizarResponse.getBody());

        assertThat(actualizarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actualizarBody.path("status").asInt()).isEqualTo(200);
        assertThat(actualizarBody.path("data").path("nombre").asText()).isEqualTo(ROL_NOMBRE_ACTUALIZADO);

        ResponseEntity<String> duplicadoResponse = crearRol(accessToken, ROL_NOMBRE_ACTUALIZADO);

        assertThat(duplicadoResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<String> eliminarResponse = eliminarRol(accessToken, rolId);

        assertThat(eliminarResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(consultarRol(accessToken, rolId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(consultarActivoEnBaseDeDatos(rolId)).isFalse();
    }

    /**
     * Crea un usuario mediante el endpoint publico y devuelve su identificador.
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
     * Asigna el rol ADMIN directamente en la tabla intermedia para simular el bootstrap controlado.
     */
    private void asignarRolAdmin(Long usuarioId) {
        jdbcTemplate.update("""
                INSERT INTO usuario_rol (usuario_id, rol_id)
                SELECT ?, rol.id
                FROM rol
                WHERE rol.nombre = 'ADMIN'
                ON CONFLICT DO NOTHING
                """,
                usuarioId
        );
    }

    /**
     * Inicia sesion con un usuario existente y devuelve el JWT emitido por el backend.
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
     * Consulta el listado paginado de roles usando autenticacion Bearer.
     */
    private ResponseEntity<String> listarRoles(String accessToken) {
        return restTemplate.exchange(
                "/api/v1/roles?page=0&size=10&sort=nombre,asc",
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Crea un rol activo con el nombre recibido usando autenticacion Bearer.
     */
    private ResponseEntity<String> crearRol(String accessToken, String nombre) {
        RolRequest request = new RolRequest(nombre, true);

        return restTemplate.postForEntity(
                "/api/v1/roles",
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Consulta un rol por identificador usando autenticacion Bearer.
     */
    private ResponseEntity<String> consultarRol(String accessToken, Long rolId) {
        return restTemplate.exchange(
                "/api/v1/roles/" + rolId,
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Actualiza el nombre de un rol existente usando autenticacion Bearer.
     */
    private ResponseEntity<String> actualizarRol(String accessToken, Long rolId, String nombre) {
        RolRequest request = new RolRequest(nombre, true);

        return restTemplate.exchange(
                "/api/v1/roles/" + rolId,
                HttpMethod.PUT,
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Elimina logicamente un rol por identificador usando autenticacion Bearer.
     */
    private ResponseEntity<String> eliminarRol(String accessToken, Long rolId) {
        return restTemplate.exchange(
                "/api/v1/roles/" + rolId,
                HttpMethod.DELETE,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Crea headers HTTP con Content-Type JSON y el JWT en formato Bearer.
     */
    private HttpHeaders crearHeadersConJwt(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    /**
     * Consulta directamente la bandera activo para confirmar el borrado logico.
     */
    private Boolean consultarActivoEnBaseDeDatos(Long rolId) {
        return jdbcTemplate.queryForObject(
                "SELECT activo FROM rol WHERE id = ?",
                Boolean.class,
                rolId
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

