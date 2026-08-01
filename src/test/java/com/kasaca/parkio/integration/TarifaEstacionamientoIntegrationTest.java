package com.kasaca.parkio.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoRequest;
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
 * Pruebas de integracion para validar el modulo Tarifa con la aplicacion completa.
 *
 * <p>Estas pruebas levantan Spring Boot, ejecutan Flyway contra PostgreSQL,
 * consumen endpoints HTTP reales y validan seguridad por rol junto con el flujo
 * principal de administracion de tarifas por estacionamiento.</p>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TarifaEstacionamientoIntegrationTest {

    private static final String TEST_DATABASE_NAME = "parkio_test";
    private static final String ADMIN_EMAIL = "integration.admin.tarifa@parkio.com";
    private static final String OWNER_EMAIL = "integration.owner.tarifa@parkio.com";
    private static final String OTRO_OWNER_EMAIL = "integration.otro-owner.tarifa@parkio.com";
    private static final String USER_EMAIL = "integration.user.tarifa@parkio.com";
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
                    tarifa_estacionamiento,
                    usuario_rol,
                    usuario_estacionamiento,
                    cajon,
                    estacionamiento,
                    usuario
                RESTART IDENTITY
                CASCADE
                """);

        asegurarRolesBase();
    }

    /**
     * Verifica que los endpoints de tarifa rechacen solicitudes sin JWT.
     */
    @Test
    void debeRechazarConsultaDeTarifaSinToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/tarifas/estacionamiento/1",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Verifica que un usuario con rol USER no pueda administrar tarifas.
     */
    @Test
    void debeRechazarAdministracionConUsuarioUser() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos("Parkio User");

        registrarUsuario("Usuario", USER_EMAIL);
        String userToken = iniciarSesion(USER_EMAIL);

        ResponseEntity<String> crearResponse = crearTarifa(userToken, estacionamientoId);

        assertThat(crearResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Valida el flujo completo de creacion, consulta, actualizacion y eliminacion logica con rol ADMIN.
     */
    @Test
    void debeAdministrarTarifasConJwtAdmin() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos("Parkio Tarifa Admin");

        Long adminId = registrarUsuario("Administrador", ADMIN_EMAIL);
        asignarRol(adminId, "ADMIN");
        String adminToken = iniciarSesion(ADMIN_EMAIL);

        ResponseEntity<String> crearResponse = crearTarifa(adminToken, estacionamientoId);
        JsonNode crearBody = objectMapper.readTree(crearResponse.getBody());

        assertThat(crearResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(crearBody.path("status").asInt()).isEqualTo(201);
        assertThat(crearBody.path("transactionId").asText()).isNotBlank();
        assertThat(crearBody.path("data").path("estacionamientoId").asLong()).isEqualTo(estacionamientoId);
        assertThat(crearBody.path("data").path("precioPorHora").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(crearBody.path("data").path("activo").asBoolean()).isTrue();

        ResponseEntity<String> duplicadoResponse = crearTarifa(adminToken, estacionamientoId);
        assertThat(duplicadoResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<String> consultarResponse = consultarTarifa(adminToken, estacionamientoId);
        JsonNode consultarBody = objectMapper.readTree(consultarResponse.getBody());

        assertThat(consultarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultarBody.path("data").path("estacionamientoId").asLong()).isEqualTo(estacionamientoId);

        ResponseEntity<String> actualizarResponse = actualizarTarifa(adminToken, estacionamientoId);
        JsonNode actualizarBody = objectMapper.readTree(actualizarResponse.getBody());

        assertThat(actualizarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actualizarBody.path("data").path("precioPorHora").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(actualizarBody.path("data").path("minutosTolerancia").asInt()).isEqualTo(15);
        assertThat(actualizarBody.path("data").path("cobrarFraccion").asBoolean()).isFalse();
        assertThat(actualizarBody.path("data").path("tarifaMinima").decimalValue()).isEqualByComparingTo("20.00");

        ResponseEntity<String> eliminarResponse = eliminarTarifa(adminToken, estacionamientoId);

        assertThat(eliminarResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(consultarActivoTarifaEnBaseDeDatos(estacionamientoId)).isFalse();
        assertThat(consultarTarifa(adminToken, estacionamientoId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * Valida que OWNER solo pueda administrar tarifas de sus propios estacionamientos.
     */
    @Test
    void debeLimitarTarifasAEstacionamientosDelOwner() throws Exception {
        Long ownerId = registrarUsuario("Owner", OWNER_EMAIL);
        asignarRol(ownerId, "OWNER");
        String ownerToken = iniciarSesion(OWNER_EMAIL);

        Long otroOwnerId = registrarUsuario("Otro Owner", OTRO_OWNER_EMAIL);
        asignarRol(otroOwnerId, "OWNER");
        String otroOwnerToken = iniciarSesion(OTRO_OWNER_EMAIL);

        Long estacionamientoPropioId = crearEstacionamientoConOwnerEnBaseDeDatos(
                "Parkio Owner",
                ownerId
        );
        Long estacionamientoAjenoId = crearEstacionamientoConOwnerEnBaseDeDatos(
                "Parkio Ajeno",
                otroOwnerId
        );

        ResponseEntity<String> crearPropioResponse = crearTarifa(ownerToken, estacionamientoPropioId);
        ResponseEntity<String> crearAjenoResponse = crearTarifa(ownerToken, estacionamientoAjenoId);

        assertThat(crearPropioResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(crearAjenoResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> consultarPropioResponse = consultarTarifa(ownerToken, estacionamientoPropioId);
        ResponseEntity<String> consultarAjenoResponse = consultarTarifa(ownerToken, estacionamientoAjenoId);
        ResponseEntity<String> actualizarAjenoResponse = actualizarTarifa(ownerToken, estacionamientoAjenoId);
        ResponseEntity<String> eliminarAjenoResponse = eliminarTarifa(ownerToken, estacionamientoAjenoId);

        assertThat(consultarPropioResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultarAjenoResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(actualizarAjenoResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(eliminarAjenoResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> crearAjenoConSuOwnerResponse = crearTarifa(otroOwnerToken, estacionamientoAjenoId);
        assertThat(crearAjenoConSuOwnerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    /**
     * Registra un usuario por el endpoint publico para obtener credenciales reales de login.
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
     * Asigna el rol indicado directamente para simular el bootstrap controlado de permisos.
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
     * Asegura que los roles base existan y esten activos en la base de pruebas.
     */
    private void asegurarRolesBase() {
        jdbcTemplate.update("""
                INSERT INTO rol (nombre, activo)
                VALUES
                    ('ADMIN', TRUE),
                    ('OWNER', TRUE),
                    ('OPERADOR', TRUE),
                    ('USER', TRUE)
                ON CONFLICT (nombre)
                DO UPDATE SET activo = EXCLUDED.activo
                """);
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
     * Crea una tarifa activa usando autenticacion Bearer.
     */
    private ResponseEntity<String> crearTarifa(String accessToken, Long estacionamientoId) {
        return restTemplate.postForEntity(
                "/api/v1/tarifas",
                new HttpEntity<>(crearTarifaRequest(estacionamientoId), crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Consulta una tarifa por estacionamiento usando autenticacion Bearer.
     */
    private ResponseEntity<String> consultarTarifa(String accessToken, Long estacionamientoId) {
        return restTemplate.exchange(
                "/api/v1/tarifas/estacionamiento/" + estacionamientoId,
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Actualiza una tarifa activa usando autenticacion Bearer.
     */
    private ResponseEntity<String> actualizarTarifa(String accessToken, Long estacionamientoId) {
        TarifaEstacionamientoRequest request = new TarifaEstacionamientoRequest(
                estacionamientoId,
                new BigDecimal("30.00"),
                15,
                false,
                new BigDecimal("20.00")
        );

        return restTemplate.exchange(
                "/api/v1/tarifas/estacionamiento/" + estacionamientoId,
                HttpMethod.PUT,
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Elimina logicamente una tarifa activa usando autenticacion Bearer.
     */
    private ResponseEntity<String> eliminarTarifa(String accessToken, Long estacionamientoId) {
        return restTemplate.exchange(
                "/api/v1/tarifas/estacionamiento/" + estacionamientoId,
                HttpMethod.DELETE,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Construye un request valido para crear tarifas.
     */
    private TarifaEstacionamientoRequest crearTarifaRequest(Long estacionamientoId) {
        return new TarifaEstacionamientoRequest(
                estacionamientoId,
                new BigDecimal("25.00"),
                10,
                true,
                new BigDecimal("15.00")
        );
    }

    /**
     * Crea un estacionamiento activo sin owner directamente en base para pruebas con ADMIN.
     */
    private Long crearEstacionamientoActivoEnBaseDeDatos(String nombre) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO estacionamiento (nombre, descripcion, latitud, longitud, activo)
                VALUES (?, 'Dato de apoyo para tarifa', 19.43260800, -99.13320900, TRUE)
                RETURNING id
                """,
                Long.class,
                nombre
        );
    }

    /**
     * Crea un estacionamiento activo con owner directamente en base para validar alcance OWNER.
     */
    private Long crearEstacionamientoConOwnerEnBaseDeDatos(String nombre, Long ownerId) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO estacionamiento (nombre, descripcion, latitud, longitud, owner_id, activo)
                VALUES (?, 'Dato de apoyo para tarifa owner', 19.43260800, -99.13320900, ?, TRUE)
                RETURNING id
                """,
                Long.class,
                nombre,
                ownerId
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
     * Consulta directamente la bandera activo de la tarifa para confirmar el borrado logico.
     */
    private Boolean consultarActivoTarifaEnBaseDeDatos(Long estacionamientoId) {
        return jdbcTemplate.queryForObject(
                "SELECT activo FROM tarifa_estacionamiento WHERE estacionamiento_id = ?",
                Boolean.class,
                estacionamientoId
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
