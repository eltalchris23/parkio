package com.kasaca.parkio.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.usuario.dto.UsuarioCreateRequest;
import com.kasaca.parkio.usuario.dto.UsuarioEstacionamientoRequest;
import com.kasaca.parkio.usuario.dto.UsuarioPasswordRequest;
import com.kasaca.parkio.usuario.dto.UsuarioRolRequest;
import com.kasaca.parkio.usuario.dto.UsuarioUpdateRequest;
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
 * Pruebas de integracion para validar el modulo Usuario con la aplicacion completa.
 *
 * <p>Estas pruebas levantan Spring Boot, ejecutan Flyway contra PostgreSQL,
 * consumen endpoints HTTP reales y validan seguridad JWT, autorizacion por roles,
 * administracion de relaciones, cambio de contraseña y borrado logico.</p>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsuarioIntegrationTest {

    private static final String TEST_DATABASE_NAME = "parkio_test";
    private static final String ADMIN_EMAIL = "integration.admin.usuario@parkio.com";
    private static final String USER_EMAIL = "integration.user.usuario@parkio.com";
    private static final String OTHER_USER_EMAIL = "integration.other.usuario@parkio.com";
    private static final String PASSWORD = "clave-integracion";
    private static final String NEW_PASSWORD = "nueva-clave-integracion";

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
     * Verifica que el listado administrativo de usuarios rechace solicitudes sin JWT.
     */
    @Test
    void debeRechazarListadoDeUsuariosSinToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/usuarios",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Verifica que un usuario con rol USER no pueda consultar el listado administrativo.
     */
    @Test
    void debeRechazarListadoDeUsuariosConUsuarioSinRolAdmin() throws Exception {
        registrarUsuario("Usuario", USER_EMAIL, PASSWORD);
        String accessToken = iniciarSesion(USER_EMAIL, PASSWORD);

        ResponseEntity<String> response = listarUsuarios(accessToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Valida que la creacion publica asigne el rol USER y rechace correos duplicados.
     */
    @Test
    void debeCrearUsuarioPublicoConRolUserYRechazarCorreoDuplicado() throws Exception {
        ResponseEntity<String> crearResponse = crearUsuario("Usuario", USER_EMAIL, PASSWORD);
        JsonNode crearBody = objectMapper.readTree(crearResponse.getBody());

        assertThat(crearResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(crearBody.path("status").asInt()).isEqualTo(201);
        assertThat(crearBody.path("transactionId").asText()).isNotBlank();
        assertThat(crearBody.path("data").path("email").asText()).isEqualTo(USER_EMAIL);
        assertThat(crearBody.path("data").path("roles").get(0).asText()).isEqualTo("USER");
        assertThat(crearBody.path("data").path("passwordHash").isMissingNode()).isTrue();

        ResponseEntity<String> duplicadoResponse = crearUsuario("Usuario Duplicado", USER_EMAIL, PASSWORD);

        assertThat(duplicadoResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    /**
     * Verifica que un usuario autenticado pueda consultar y actualizar solamente su propio registro.
     */
    @Test
    void debePermitirConsultarYActualizarUsuarioPropioYBloquearUsuarioAjeno() throws Exception {
        Long usuarioId = registrarUsuario("Usuario", USER_EMAIL, PASSWORD);
        Long otroUsuarioId = registrarUsuario("Otro", OTHER_USER_EMAIL, PASSWORD);
        String accessToken = iniciarSesion(USER_EMAIL, PASSWORD);

        ResponseEntity<String> consultarPropioResponse = consultarUsuario(accessToken, usuarioId);
        ResponseEntity<String> consultarAjenoResponse = consultarUsuario(accessToken, otroUsuarioId);

        assertThat(consultarPropioResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultarAjenoResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> actualizarPropioResponse = actualizarUsuario(
                accessToken,
                usuarioId,
                "Usuario Actualizado",
                USER_EMAIL
        );
        JsonNode actualizarBody = objectMapper.readTree(actualizarPropioResponse.getBody());
        ResponseEntity<String> actualizarAjenoResponse = actualizarUsuario(
                accessToken,
                otroUsuarioId,
                "No Permitido",
                OTHER_USER_EMAIL
        );

        assertThat(actualizarPropioResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actualizarBody.path("data").path("nombre").asText()).isEqualTo("Usuario Actualizado");
        assertThat(actualizarAjenoResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Valida que el usuario pueda cambiar su contraseña y autenticarse con la nueva.
     */
    @Test
    void debeActualizarPasswordDelUsuarioPropioYPermitirLoginConNuevaPassword() throws Exception {
        Long usuarioId = registrarUsuario("Usuario", USER_EMAIL, PASSWORD);
        String accessToken = iniciarSesion(USER_EMAIL, PASSWORD);

        ResponseEntity<String> passwordResponse = actualizarPassword(accessToken, usuarioId, NEW_PASSWORD);

        assertThat(passwordResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(iniciarSesion(USER_EMAIL, NEW_PASSWORD)).isNotBlank();
    }

    /**
     * Valida el flujo administrativo de listar, asignar y retirar roles y estacionamientos.
     */
    @Test
    void debeAdministrarUsuariosRolesYEstacionamientosConJwtAdmin() throws Exception {
        Long adminId = registrarUsuario("Administrador", ADMIN_EMAIL, PASSWORD);
        Long usuarioId = registrarUsuario("Usuario", USER_EMAIL, PASSWORD);
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long operadorRolId = consultarRolId("OPERADOR");
        asignarRol(adminId, "ADMIN");
        String adminToken = iniciarSesion(ADMIN_EMAIL, PASSWORD);

        ResponseEntity<String> listadoResponse = listarUsuarios(adminToken);
        JsonNode listadoBody = objectMapper.readTree(listadoResponse.getBody());

        assertThat(listadoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listadoBody.path("status").asInt()).isEqualTo(200);
        assertThat(listadoBody.path("transactionId").asText()).isNotBlank();
        assertThat(listadoBody.path("data").path("content").size()).isGreaterThanOrEqualTo(2);

        ResponseEntity<String> asignarRolResponse = asignarRolPorEndpoint(adminToken, usuarioId, operadorRolId);
        JsonNode asignarRolBody = objectMapper.readTree(asignarRolResponse.getBody());

        assertThat(asignarRolResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(asignarRolBody.path("data").path("roles").toString()).contains("OPERADOR");

        ResponseEntity<String> rolDuplicadoResponse = asignarRolPorEndpoint(adminToken, usuarioId, operadorRolId);

        assertThat(rolDuplicadoResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<String> asignarEstacionamientoResponse = asignarEstacionamiento(
                adminToken,
                usuarioId,
                estacionamientoId
        );
        JsonNode asignarEstacionamientoBody = objectMapper.readTree(asignarEstacionamientoResponse.getBody());

        assertThat(asignarEstacionamientoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(asignarEstacionamientoBody.path("data").path("estacionamientoIds").toString())
                .contains(estacionamientoId.toString());

        ResponseEntity<String> estacionamientoDuplicadoResponse = asignarEstacionamiento(
                adminToken,
                usuarioId,
                estacionamientoId
        );

        assertThat(estacionamientoDuplicadoResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<String> retirarRolResponse = retirarRol(adminToken, usuarioId, operadorRolId);
        ResponseEntity<String> retirarEstacionamientoResponse = retirarEstacionamiento(
                adminToken,
                usuarioId,
                estacionamientoId
        );

        assertThat(retirarRolResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(retirarEstacionamientoResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    /**
     * Verifica que el borrado logico desactive el usuario y bloquee inicios de sesion posteriores.
     */
    @Test
    void debeEliminarUsuarioLogicamenteYBloquearLoginPosterior() throws Exception {
        Long adminId = registrarUsuario("Administrador", ADMIN_EMAIL, PASSWORD);
        Long usuarioId = registrarUsuario("Usuario", USER_EMAIL, PASSWORD);
        asignarRol(adminId, "ADMIN");
        String adminToken = iniciarSesion(ADMIN_EMAIL, PASSWORD);

        ResponseEntity<String> eliminarResponse = eliminarUsuario(adminToken, usuarioId);

        assertThat(eliminarResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(consultarUsuario(adminToken, usuarioId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(consultarActivoUsuarioEnBaseDeDatos(usuarioId)).isFalse();
        assertThat(login(USER_EMAIL, PASSWORD).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Crea un usuario mediante el endpoint publico y devuelve su identificador.
     */
    private Long registrarUsuario(String nombre, String email, String password) throws Exception {
        ResponseEntity<String> response = crearUsuario(nombre, email, password);
        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return body.path("data").path("id").asLong();
    }

    /**
     * Ejecuta el POST publico de usuarios con un request valido.
     */
    private ResponseEntity<String> crearUsuario(String nombre, String email, String password) {
        UsuarioCreateRequest request = new UsuarioCreateRequest(
                nombre,
                "Integracion",
                email,
                password
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate.postForEntity(
                "/api/usuarios",
                new HttpEntity<>(request, headers),
                String.class
        );
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
     * Inicia sesion y devuelve solamente el JWT emitido por el backend.
     */
    private String iniciarSesion(String email, String password) throws Exception {
        ResponseEntity<String> response = login(email, password);
        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return body.path("accessToken").asText();
    }

    /**
     * Ejecuta el endpoint de login y devuelve la respuesta HTTP completa.
     */
    private ResponseEntity<String> login(String email, String password) {
        AuthLoginRequest request = new AuthLoginRequest(email, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(request, headers),
                String.class
        );
    }

    /**
     * Consulta el listado paginado de usuarios usando autenticacion Bearer.
     */
    private ResponseEntity<String> listarUsuarios(String accessToken) {
        return restTemplate.exchange(
                "/api/usuarios?page=0&size=10&sort=email,asc",
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Consulta un usuario por identificador usando autenticacion Bearer.
     */
    private ResponseEntity<String> consultarUsuario(String accessToken, Long usuarioId) {
        return restTemplate.exchange(
                "/api/usuarios/" + usuarioId,
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Actualiza los datos generales de un usuario usando autenticacion Bearer.
     */
    private ResponseEntity<String> actualizarUsuario(
            String accessToken,
            Long usuarioId,
            String nombre,
            String email
    ) {
        UsuarioUpdateRequest request = new UsuarioUpdateRequest(
                nombre,
                "Integracion Actualizado",
                email
        );

        return restTemplate.exchange(
                "/api/usuarios/" + usuarioId,
                HttpMethod.PUT,
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Cambia la contraseña de un usuario usando autenticacion Bearer.
     */
    private ResponseEntity<String> actualizarPassword(String accessToken, Long usuarioId, String nuevaPassword) {
        UsuarioPasswordRequest request = new UsuarioPasswordRequest(nuevaPassword);

        return restTemplate.exchange(
                "/api/usuarios/" + usuarioId + "/password",
                HttpMethod.PATCH,
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Asigna un rol existente a un usuario mediante el endpoint administrativo.
     */
    private ResponseEntity<String> asignarRolPorEndpoint(String accessToken, Long usuarioId, Long rolId) {
        UsuarioRolRequest request = new UsuarioRolRequest(rolId);

        return restTemplate.postForEntity(
                "/api/usuarios/" + usuarioId + "/roles",
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Retira un rol de un usuario mediante el endpoint administrativo.
     */
    private ResponseEntity<String> retirarRol(String accessToken, Long usuarioId, Long rolId) {
        return restTemplate.exchange(
                "/api/usuarios/" + usuarioId + "/roles/" + rolId,
                HttpMethod.DELETE,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Asigna un estacionamiento activo a un usuario mediante el endpoint administrativo.
     */
    private ResponseEntity<String> asignarEstacionamiento(
            String accessToken,
            Long usuarioId,
            Long estacionamientoId
    ) {
        UsuarioEstacionamientoRequest request = new UsuarioEstacionamientoRequest(estacionamientoId);

        return restTemplate.postForEntity(
                "/api/usuarios/" + usuarioId + "/estacionamientos",
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Retira un estacionamiento de un usuario mediante el endpoint administrativo.
     */
    private ResponseEntity<String> retirarEstacionamiento(
            String accessToken,
            Long usuarioId,
            Long estacionamientoId
    ) {
        return restTemplate.exchange(
                "/api/usuarios/" + usuarioId + "/estacionamientos/" + estacionamientoId,
                HttpMethod.DELETE,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Elimina logicamente un usuario por identificador usando autenticacion Bearer.
     */
    private ResponseEntity<String> eliminarUsuario(String accessToken, Long usuarioId) {
        return restTemplate.exchange(
                "/api/usuarios/" + usuarioId,
                HttpMethod.DELETE,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Crea un estacionamiento activo directamente en base para probar asignaciones de usuario.
     */
    private Long crearEstacionamientoActivoEnBaseDeDatos() {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO estacionamiento (nombre, descripcion, latitud, longitud, activo)
                VALUES ('Estacionamiento Usuario Integracion', 'Dato de apoyo para pruebas', 19.43260800, -99.13320900, TRUE)
                RETURNING id
                """,
                Long.class
        );
    }

    /**
     * Consulta el identificador de un rol base por nombre sin asumir IDs fijos.
     */
    private Long consultarRolId(String rolNombre) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM rol WHERE nombre = ? AND activo = TRUE",
                Long.class,
                rolNombre
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
     * Consulta directamente la bandera activo del usuario para confirmar el borrado logico.
     */
    private Boolean consultarActivoUsuarioEnBaseDeDatos(Long usuarioId) {
        return jdbcTemplate.queryForObject(
                "SELECT activo FROM usuario WHERE id = ?",
                Boolean.class,
                usuarioId
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
