package com.kasaca.parkio.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integracion para validar el modulo Estacionamiento con la aplicacion completa.
 *
 * <p>Estas pruebas levantan Spring Boot, ejecutan Flyway contra PostgreSQL,
 * consumen endpoints HTTP reales y validan la autorizacion por roles junto con
 * el flujo CRUD de estacionamientos.</p>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EstacionamientoIntegrationTest {

    private static final String TEST_DATABASE_NAME = "parkio_test";
    private static final String ADMIN_EMAIL = "integration.admin.estacionamiento@parkio.com";
    private static final String USER_EMAIL = "integration.user.estacionamiento@parkio.com";
    private static final String PASSWORD = "clave-integracion";
    private static final String ESTACIONAMIENTO_NOMBRE = "Estacionamiento Integracion";
    private static final String ESTACIONAMIENTO_NOMBRE_ACTUALIZADO = "Estacionamiento Integracion Actualizado";

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
                WHERE nombre IN ('ADMIN', 'OPERADOR', 'USER')
                """);
    }

    /**
     * Verifica que el endpoint protegido de estacionamientos rechace solicitudes sin JWT.
     */
    @Test
    void debeRechazarConsultaDeEstacionamientosSinToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/estacionamientos",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Verifica que un usuario autenticado con rol USER pueda consultar estacionamientos.
     */
    @Test
    void debePermitirConsultaDeEstacionamientosConUsuarioUser() throws Exception {
        registrarUsuario("Usuario", USER_EMAIL);
        String accessToken = iniciarSesion(USER_EMAIL);

        ResponseEntity<String> response = listarEstacionamientos(accessToken);
        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.path("status").asInt()).isEqualTo(200);
        assertThat(body.path("transactionId").asText()).isNotBlank();
        assertThat(body.path("data").path("content").isArray()).isTrue();
    }

    /**
     * Verifica que un usuario USER no pueda crear estacionamientos porque la escritura es solo ADMIN.
     */
    @Test
    void debeRechazarCreacionDeEstacionamientoConUsuarioUser() throws Exception {
        registrarUsuario("Usuario", USER_EMAIL);
        String accessToken = iniciarSesion(USER_EMAIL);

        ResponseEntity<String> response = crearEstacionamiento(accessToken, ESTACIONAMIENTO_NOMBRE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Valida el flujo completo del CRUD de estacionamientos usando un JWT con rol ADMIN.
     */
    @Test
    void debeAdministrarEstacionamientosConJwtAdmin() throws Exception {
        Long adminId = registrarUsuario("Administrador", ADMIN_EMAIL);
        asignarRolAdmin(adminId);
        String accessToken = iniciarSesion(ADMIN_EMAIL);

        ResponseEntity<String> listadoResponse = listarEstacionamientos(accessToken);
        JsonNode listadoBody = objectMapper.readTree(listadoResponse.getBody());

        assertThat(listadoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listadoBody.path("status").asInt()).isEqualTo(200);
        assertThat(listadoBody.path("transactionId").asText()).isNotBlank();
        assertThat(listadoBody.path("data").path("content").isArray()).isTrue();

        ResponseEntity<String> crearResponse = crearEstacionamiento(accessToken, ESTACIONAMIENTO_NOMBRE);
        JsonNode crearBody = objectMapper.readTree(crearResponse.getBody());
        Long estacionamientoId = crearBody.path("data").path("id").asLong();

        assertThat(crearResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(crearBody.path("status").asInt()).isEqualTo(201);
        assertThat(crearBody.path("transactionId").asText()).isNotBlank();
        assertThat(crearBody.path("data").path("nombre").asText()).isEqualTo(ESTACIONAMIENTO_NOMBRE);
        assertThat(crearBody.path("data").path("activo").asBoolean()).isTrue();

        ResponseEntity<String> consultarResponse = consultarEstacionamiento(accessToken, estacionamientoId);
        JsonNode consultarBody = objectMapper.readTree(consultarResponse.getBody());

        assertThat(consultarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultarBody.path("data").path("id").asLong()).isEqualTo(estacionamientoId);
        assertThat(consultarBody.path("data").path("nombre").asText()).isEqualTo(ESTACIONAMIENTO_NOMBRE);

        ResponseEntity<String> actualizarResponse = actualizarEstacionamiento(
                accessToken,
                estacionamientoId,
                ESTACIONAMIENTO_NOMBRE_ACTUALIZADO
        );
        JsonNode actualizarBody = objectMapper.readTree(actualizarResponse.getBody());

        assertThat(actualizarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actualizarBody.path("status").asInt()).isEqualTo(200);
        assertThat(actualizarBody.path("data").path("nombre").asText()).isEqualTo(ESTACIONAMIENTO_NOMBRE_ACTUALIZADO);

        Long cajonId = crearCajonActivoEnBaseDeDatos(estacionamientoId);

        ResponseEntity<String> eliminarResponse = eliminarEstacionamiento(accessToken, estacionamientoId);

        assertThat(eliminarResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(consultarEstacionamiento(accessToken, estacionamientoId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(consultarActivoEstacionamientoEnBaseDeDatos(estacionamientoId)).isFalse();
        assertThat(consultarActivoCajonEnBaseDeDatos(cajonId)).isFalse();
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
     * Asigna el rol ADMIN directamente para simular el bootstrap controlado de administracion.
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
     * Consulta el listado paginado de estacionamientos usando autenticacion Bearer.
     */
    private ResponseEntity<String> listarEstacionamientos(String accessToken) {
        return restTemplate.exchange(
                "/api/v1/estacionamientos?page=0&size=10&sort=nombre,asc",
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Crea un estacionamiento activo con el nombre recibido usando autenticacion Bearer.
     */
    private ResponseEntity<String> crearEstacionamiento(String accessToken, String nombre) {
        EstacionamientoRequest request = crearEstacionamientoRequest(nombre);

        return restTemplate.postForEntity(
                "/api/v1/estacionamientos",
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Consulta un estacionamiento por identificador usando autenticacion Bearer.
     */
    private ResponseEntity<String> consultarEstacionamiento(String accessToken, Long estacionamientoId) {
        return restTemplate.exchange(
                "/api/v1/estacionamientos/" + estacionamientoId,
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Actualiza los datos de un estacionamiento existente usando autenticacion Bearer.
     */
    private ResponseEntity<String> actualizarEstacionamiento(
            String accessToken,
            Long estacionamientoId,
            String nombre
    ) {
        EstacionamientoRequest request = crearEstacionamientoRequest(nombre);

        return restTemplate.exchange(
                "/api/v1/estacionamientos/" + estacionamientoId,
                HttpMethod.PUT,
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Elimina logicamente un estacionamiento por identificador usando autenticacion Bearer.
     */
    private ResponseEntity<String> eliminarEstacionamiento(String accessToken, Long estacionamientoId) {
        return restTemplate.exchange(
                "/api/v1/estacionamientos/" + estacionamientoId,
                HttpMethod.DELETE,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Construye un request valido de estacionamiento para las operaciones POST y PUT.
     */
    private EstacionamientoRequest crearEstacionamientoRequest(String nombre) {
        return new EstacionamientoRequest(
                nombre,
                "Estacionamiento creado desde prueba de integracion",
                new BigDecimal("19.43260800"),
                new BigDecimal("-99.13320900")
        );
    }

    /**
     * Crea un cajon activo asociado al estacionamiento para validar la desactivacion en cascada logica.
     */
    private Long crearCajonActivoEnBaseDeDatos(Long estacionamientoId) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO cajon (estacionamiento_id, numero, tipo, estado, activo)
                VALUES (?, 'A-01', 'AUTO', 'LIBRE', TRUE)
                RETURNING id
                """,
                Long.class,
                estacionamientoId
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
     * Consulta directamente la bandera activo del estacionamiento para confirmar el borrado logico.
     */
    private Boolean consultarActivoEstacionamientoEnBaseDeDatos(Long estacionamientoId) {
        return jdbcTemplate.queryForObject(
                "SELECT activo FROM estacionamiento WHERE id = ?",
                Boolean.class,
                estacionamientoId
        );
    }

    /**
     * Consulta directamente la bandera activo del cajon para confirmar su desactivacion logica.
     */
    private Boolean consultarActivoCajonEnBaseDeDatos(Long cajonId) {
        return jdbcTemplate.queryForObject(
                "SELECT activo FROM cajon WHERE id = ?",
                Boolean.class,
                cajonId
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

