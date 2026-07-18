package com.kasaca.parkio.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.cajon.dto.CajonEstadoRequest;
import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.entity.TipoCajon;
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
 * Pruebas de integracion para validar el modulo Cajon con la aplicacion completa.
 *
 * <p>Estas pruebas levantan Spring Boot, ejecutan Flyway contra PostgreSQL,
 * consumen endpoints HTTP reales y validan la autorizacion por roles junto con
 * el flujo principal de administracion y cambio de estado de cajones.</p>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CajonIntegrationTest {

    private static final String TEST_DATABASE_NAME = "parkio_test";
    private static final String ADMIN_EMAIL = "integration.admin.cajon@parkio.com";
    private static final String USER_EMAIL = "integration.user.cajon@parkio.com";
    private static final String OPERADOR_EMAIL = "integration.operador.cajon@parkio.com";
    private static final String PASSWORD = "clave-integracion";
    private static final String CAJON_NUMERO = "A-01";
    private static final String CAJON_NUMERO_ACTUALIZADO = "A-02";

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
     * Verifica que el endpoint protegido de cajones rechace solicitudes sin JWT.
     */
    @Test
    void debeRechazarConsultaDeCajonesSinToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/cajones",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Verifica que un usuario con rol USER pueda consultar cajones pero no pueda crearlos.
     */
    @Test
    void debePermitirConsultaYRechazarCreacionConUsuarioUser() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long cajonId = crearCajonActivoEnBaseDeDatos(estacionamientoId, CAJON_NUMERO);

        registrarUsuario("Usuario", USER_EMAIL);
        String accessToken = iniciarSesion(USER_EMAIL);

        ResponseEntity<String> listarResponse = listarCajones(accessToken);
        ResponseEntity<String> consultarResponse = consultarCajon(accessToken, cajonId);
        ResponseEntity<String> crearResponse = crearCajon(accessToken, estacionamientoId, "U-01");

        assertThat(listarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(crearResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Verifica que un operador pueda cambiar el estado de un cajon pero no pueda administrarlo.
     */
    @Test
    void debePermitirCambioDeEstadoYRechazarAdministracionConOperador() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long cajonId = crearCajonActivoEnBaseDeDatos(estacionamientoId, CAJON_NUMERO);

        Long operadorId = registrarUsuario("Operador", OPERADOR_EMAIL);
        asignarRol(operadorId, "OPERADOR");
        String accessToken = iniciarSesion(OPERADOR_EMAIL);

        ResponseEntity<String> estadoResponse = actualizarEstado(accessToken, cajonId, EstadoCajon.OCUPADO);
        JsonNode estadoBody = objectMapper.readTree(estadoResponse.getBody());
        ResponseEntity<String> actualizarResponse = actualizarCajon(accessToken, cajonId, estacionamientoId, CAJON_NUMERO_ACTUALIZADO);
        ResponseEntity<String> eliminarResponse = eliminarCajon(accessToken, cajonId);

        assertThat(estadoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(estadoBody.path("data").path("estado").asText()).isEqualTo("OCUPADO");
        assertThat(actualizarResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(eliminarResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Valida el flujo completo de administracion de cajones usando un JWT con rol ADMIN.
     */
    @Test
    void debeAdministrarCajonesConJwtAdmin() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long adminId = registrarUsuario("Administrador", ADMIN_EMAIL);
        asignarRol(adminId, "ADMIN");
        String accessToken = iniciarSesion(ADMIN_EMAIL);

        ResponseEntity<String> crearResponse = crearCajon(accessToken, estacionamientoId, CAJON_NUMERO);
        JsonNode crearBody = objectMapper.readTree(crearResponse.getBody());
        Long cajonId = crearBody.path("data").path("id").asLong();

        assertThat(crearResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(crearBody.path("status").asInt()).isEqualTo(201);
        assertThat(crearBody.path("transactionId").asText()).isNotBlank();
        assertThat(crearBody.path("data").path("numero").asText()).isEqualTo(CAJON_NUMERO);
        assertThat(crearBody.path("data").path("tipo").asText()).isEqualTo("AUTO");
        assertThat(crearBody.path("data").path("estado").asText()).isEqualTo("LIBRE");
        assertThat(crearBody.path("data").path("activo").asBoolean()).isTrue();

        ResponseEntity<String> listarResponse = listarCajones(accessToken);
        ResponseEntity<String> filtrarResponse = listarCajonesPorEstacionamiento(accessToken, estacionamientoId);
        ResponseEntity<String> consultarResponse = consultarCajon(accessToken, cajonId);
        JsonNode consultarBody = objectMapper.readTree(consultarResponse.getBody());

        assertThat(listarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filtrarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultarBody.path("data").path("id").asLong()).isEqualTo(cajonId);

        ResponseEntity<String> actualizarResponse = actualizarCajon(
                accessToken,
                cajonId,
                estacionamientoId,
                CAJON_NUMERO_ACTUALIZADO
        );
        JsonNode actualizarBody = objectMapper.readTree(actualizarResponse.getBody());

        assertThat(actualizarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actualizarBody.path("data").path("numero").asText()).isEqualTo(CAJON_NUMERO_ACTUALIZADO);

        ResponseEntity<String> duplicadoResponse = crearCajon(accessToken, estacionamientoId, CAJON_NUMERO_ACTUALIZADO);

        assertThat(duplicadoResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<String> estadoResponse = actualizarEstado(accessToken, cajonId, EstadoCajon.FUERA_SERVICIO);
        JsonNode estadoBody = objectMapper.readTree(estadoResponse.getBody());

        assertThat(estadoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(estadoBody.path("data").path("estado").asText()).isEqualTo("FUERA_SERVICIO");

        ResponseEntity<String> eliminarResponse = eliminarCajon(accessToken, cajonId);

        assertThat(eliminarResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(consultarCajon(accessToken, cajonId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
                "/api/usuarios",
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
     * Inicia sesion con un usuario existente y devuelve el JWT emitido por el backend.
     */
    private String iniciarSesion(String email) throws Exception {
        AuthLoginRequest request = new AuthLoginRequest(email, PASSWORD);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(request, headers),
                String.class
        );

        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return body.path("accessToken").asText();
    }

    /**
     * Consulta el listado paginado de cajones usando autenticacion Bearer.
     */
    private ResponseEntity<String> listarCajones(String accessToken) {
        return restTemplate.exchange(
                "/api/cajones?page=0&size=10&sort=numero,asc",
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Consulta el listado paginado de cajones filtrando por estacionamiento.
     */
    private ResponseEntity<String> listarCajonesPorEstacionamiento(String accessToken, Long estacionamientoId) {
        return restTemplate.exchange(
                "/api/cajones?estacionamientoId=" + estacionamientoId + "&page=0&size=10&sort=numero,asc",
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Crea un cajon activo dentro de un estacionamiento usando autenticacion Bearer.
     */
    private ResponseEntity<String> crearCajon(String accessToken, Long estacionamientoId, String numero) {
        CajonRequest request = new CajonRequest(
                numero,
                TipoCajon.AUTO,
                estacionamientoId
        );

        return restTemplate.postForEntity(
                "/api/cajones",
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Consulta un cajon por identificador usando autenticacion Bearer.
     */
    private ResponseEntity<String> consultarCajon(String accessToken, Long cajonId) {
        return restTemplate.exchange(
                "/api/cajones/" + cajonId,
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Actualiza los datos principales de un cajon usando autenticacion Bearer.
     */
    private ResponseEntity<String> actualizarCajon(
            String accessToken,
            Long cajonId,
            Long estacionamientoId,
            String numero
    ) {
        CajonRequest request = new CajonRequest(
                numero,
                TipoCajon.AUTO,
                estacionamientoId
        );

        return restTemplate.exchange(
                "/api/cajones/" + cajonId,
                HttpMethod.PUT,
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Cambia el estado operativo de un cajon usando autenticacion Bearer.
     */
    private ResponseEntity<String> actualizarEstado(
            String accessToken,
            Long cajonId,
            EstadoCajon estado
    ) {
        CajonEstadoRequest request = new CajonEstadoRequest(estado);

        return restTemplate.exchange(
                "/api/cajones/" + cajonId + "/estado",
                HttpMethod.PATCH,
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Elimina logicamente un cajon por identificador usando autenticacion Bearer.
     */
    private ResponseEntity<String> eliminarCajon(String accessToken, Long cajonId) {
        return restTemplate.exchange(
                "/api/cajones/" + cajonId,
                HttpMethod.DELETE,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Crea un estacionamiento activo directamente en la base para usarlo como dependencia del cajon.
     */
    private Long crearEstacionamientoActivoEnBaseDeDatos() {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO estacionamiento (nombre, descripcion, latitud, longitud, activo)
                VALUES ('Estacionamiento Cajon Integracion', 'Dato de apoyo para pruebas', 19.43260800, -99.13320900, TRUE)
                RETURNING id
                """,
                Long.class
        );
    }

    /**
     * Crea un cajon activo directamente en la base para preparar escenarios de consulta y permisos.
     */
    private Long crearCajonActivoEnBaseDeDatos(Long estacionamientoId, String numero) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO cajon (estacionamiento_id, numero, tipo, estado, activo)
                VALUES (?, ?, 'AUTO', 'LIBRE', TRUE)
                RETURNING id
                """,
                Long.class,
                estacionamientoId,
                numero
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
     * Consulta directamente la bandera activo del cajon para confirmar el borrado logico.
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
