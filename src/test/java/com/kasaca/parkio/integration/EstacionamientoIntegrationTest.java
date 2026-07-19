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
import java.util.Base64;

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
    private static final String OPERADOR_EMAIL = "integration.operador.estacionamiento@parkio.com";
    private static final String OWNER_EMAIL = "integration.owner.estacionamiento@parkio.com";
    private static final String OTRO_OWNER_EMAIL = "integration.otro-owner.estacionamiento@parkio.com";
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

        asegurarRolesBase();
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
     * Valida que OWNER pueda administrar únicamente sus propios estacionamientos.
     *
     * <p>El flujo crea dos usuarios con rol OWNER. Cada uno crea un estacionamiento
     * usando endpoints HTTP reales. Después se confirma que el primer OWNER puede
     * consultar, listar, actualizar y eliminar lógicamente su propio estacionamiento,
     * pero no puede operar el estacionamiento del segundo OWNER.</p>
     */
    @Test
    void debeLimitarAdministracionDeEstacionamientosAlOwnerAutenticado() throws Exception {
        Long ownerId = registrarUsuario("Owner", OWNER_EMAIL);
        asignarRol(ownerId, "OWNER");
        String ownerToken = iniciarSesion(OWNER_EMAIL);
        assertThat(extraerRolesDesdeJwt(ownerToken).toString()).contains("OWNER");

        Long otroOwnerId = registrarUsuario("Otro Owner", OTRO_OWNER_EMAIL);
        asignarRol(otroOwnerId, "OWNER");
        String otroOwnerToken = iniciarSesion(OTRO_OWNER_EMAIL);
        assertThat(extraerRolesDesdeJwt(otroOwnerToken).toString()).contains("OWNER");

        ResponseEntity<String> crearPropioResponse = crearEstacionamiento(ownerToken, "Estacionamiento Owner");
        JsonNode crearPropioBody = objectMapper.readTree(crearPropioResponse.getBody());
        Long estacionamientoPropioId = crearPropioBody.path("data").path("id").asLong();

        ResponseEntity<String> crearAjenoResponse = crearEstacionamiento(otroOwnerToken, "Estacionamiento Ajeno");
        JsonNode crearAjenoBody = objectMapper.readTree(crearAjenoResponse.getBody());
        Long estacionamientoAjenoId = crearAjenoBody.path("data").path("id").asLong();

        assertThat(crearPropioResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(crearPropioBody.path("data").path("ownerId").asLong()).isEqualTo(ownerId);
        assertThat(consultarOwnerIdEstacionamientoEnBaseDeDatos(estacionamientoPropioId)).isEqualTo(ownerId);
        assertThat(crearAjenoResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(crearAjenoBody.path("data").path("ownerId").asLong()).isEqualTo(otroOwnerId);

        ResponseEntity<String> listadoOwnerResponse = listarEstacionamientos(ownerToken);
        JsonNode listadoOwnerBody = objectMapper.readTree(listadoOwnerResponse.getBody());

        assertThat(listadoOwnerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listadoOwnerBody.path("data").path("content")).hasSize(1);
        assertThat(listadoOwnerBody.path("data").path("content").get(0).path("id").asLong())
                .isEqualTo(estacionamientoPropioId);

        ResponseEntity<String> consultarPropioResponse =
                consultarEstacionamiento(ownerToken, estacionamientoPropioId);
        ResponseEntity<String> consultarAjenoResponse =
                consultarEstacionamiento(ownerToken, estacionamientoAjenoId);

        assertThat(consultarPropioResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultarAjenoResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> actualizarPropioResponse = actualizarEstacionamiento(
                ownerToken,
                estacionamientoPropioId,
                ESTACIONAMIENTO_NOMBRE_ACTUALIZADO
        );
        ResponseEntity<String> actualizarAjenoResponse = actualizarEstacionamiento(
                ownerToken,
                estacionamientoAjenoId,
                "Intento Actualizar Ajeno"
        );

        assertThat(actualizarPropioResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actualizarAjenoResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        Long cajonPropioId = crearCajonActivoEnBaseDeDatos(estacionamientoPropioId);
        Long cajonAjenoId = crearCajonActivoEnBaseDeDatos(estacionamientoAjenoId);

        ResponseEntity<String> eliminarAjenoResponse =
                eliminarEstacionamiento(ownerToken, estacionamientoAjenoId);
        ResponseEntity<String> eliminarPropioResponse =
                eliminarEstacionamiento(ownerToken, estacionamientoPropioId);

        assertThat(eliminarAjenoResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(eliminarPropioResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(consultarActivoEstacionamientoEnBaseDeDatos(estacionamientoPropioId)).isFalse();
        assertThat(consultarActivoCajonEnBaseDeDatos(cajonPropioId)).isFalse();
        assertThat(consultarActivoEstacionamientoEnBaseDeDatos(estacionamientoAjenoId)).isTrue();
        assertThat(consultarActivoCajonEnBaseDeDatos(cajonAjenoId)).isTrue();
    }

    /**
     * Valida que OPERADOR consulte únicamente estacionamientos asignados.
     *
     * <p>La asignación se hace en usuario_estacionamiento, que es la tabla
     * existente para relacionar usuarios operativos con estacionamientos.</p>
     */
    @Test
    void debeLimitarConsultaDeEstacionamientosAlOperadorAsignado() throws Exception {
        Long estacionamientoAsignadoId =
                crearEstacionamientoActivoEnBaseDeDatos("Estacionamiento Asignado Operador");
        Long estacionamientoNoAsignadoId =
                crearEstacionamientoActivoEnBaseDeDatos("Estacionamiento No Asignado Operador");

        Long operadorId = registrarUsuario("Operador", OPERADOR_EMAIL);
        asignarRol(operadorId, "OPERADOR");
        asignarEstacionamiento(operadorId, estacionamientoAsignadoId);
        String operadorToken = iniciarSesion(OPERADOR_EMAIL);

        ResponseEntity<String> listadoResponse = listarEstacionamientos(operadorToken);
        JsonNode listadoBody = objectMapper.readTree(listadoResponse.getBody());
        ResponseEntity<String> consultarAsignadoResponse =
                consultarEstacionamiento(operadorToken, estacionamientoAsignadoId);
        ResponseEntity<String> consultarNoAsignadoResponse =
                consultarEstacionamiento(operadorToken, estacionamientoNoAsignadoId);

        assertThat(listadoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listadoBody.path("data").path("content")).hasSize(1);
        assertThat(listadoBody.path("data").path("content").get(0).path("id").asLong())
                .isEqualTo(estacionamientoAsignadoId);
        assertThat(consultarAsignadoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultarNoAsignadoResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
        asignarRol(usuarioId, "ADMIN");
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
     * Asigna un estacionamiento a un usuario usando la tabla intermedia existente.
     */
    private void asignarEstacionamiento(Long usuarioId, Long estacionamientoId) {
        jdbcTemplate.update("""
                INSERT INTO usuario_estacionamiento (usuario_id, estacionamiento_id)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """,
                usuarioId,
                estacionamientoId
        );
    }

    /**
     * Asegura que los roles base existan y esten activos en la base de pruebas.
     *
     * <p>Esto evita falsos negativos cuando parkio_test ya existia antes de una
     * migracion de datos, como la que agrego el rol OWNER.</p>
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
     * Decodifica el payload del JWT para comprobar los roles emitidos en pruebas.
     */
    private JsonNode extraerRolesDesdeJwt(String accessToken) throws Exception {
        String payload = accessToken.split("\\.")[1];
        byte[] decodedPayload = Base64.getUrlDecoder().decode(payload);
        JsonNode jwtBody = objectMapper.readTree(decodedPayload);

        return jwtBody.path("roles");
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
     * Crea un estacionamiento activo directamente en la base para escenarios de alcance.
     */
    private Long crearEstacionamientoActivoEnBaseDeDatos(String nombre) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO estacionamiento (nombre, descripcion, latitud, longitud, activo)
                VALUES (?, 'Dato de apoyo para pruebas de operador', 19.43260800, -99.13320900, TRUE)
                RETURNING id
                """,
                Long.class,
                nombre
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
     * Consulta directamente el owner_id para verificar la propiedad del estacionamiento.
     */
    private Long consultarOwnerIdEstacionamientoEnBaseDeDatos(Long estacionamientoId) {
        return jdbcTemplate.queryForObject(
                "SELECT owner_id FROM estacionamiento WHERE id = ?",
                Long.class,
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

