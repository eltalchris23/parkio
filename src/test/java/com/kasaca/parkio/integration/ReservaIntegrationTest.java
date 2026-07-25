package com.kasaca.parkio.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.reserva.dto.ReservaRequest;
import com.kasaca.parkio.reserva.service.ReservaService;
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
 * Pruebas de integracion para validar el modulo Reserva con la aplicacion completa.
 *
 * <p>Estas pruebas levantan Spring Boot, usan PostgreSQL con perfil test,
 * consumen endpoints HTTP reales y verifican el flujo inicial de reservas.</p>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservaIntegrationTest {

    private static final String TEST_DATABASE_NAME = "parkio_test";
    private static final String USER_EMAIL = "integration.user.reserva@parkio.com";
    private static final String OPERADOR_EMAIL = "integration.operador.reserva@parkio.com";
    private static final String ADMIN_EMAIL = "integration.admin.reserva@parkio.com";
    private static final String PASSWORD = "clave-integracion";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReservaService reservaService;

    /**
     * Limpia los datos variables de la prueba y conserva los roles base creados por Flyway.
     *
     * <p>Antes de limpiar valida que la conexion apunte a parkio_test para evitar
     * borrar informacion real por una mala configuracion local.</p>
     */
    @BeforeEach
    void limpiarDatosDePrueba() {
        validarBaseDeDatosDePrueba();

        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    usuario_rol,
                    usuario_estacionamiento,
                    reserva,
                    cajon,
                    estacionamiento,
                    usuario
                RESTART IDENTITY
                CASCADE
                """);

        asegurarRolesBase();
    }

    /**
     * Verifica que el endpoint de creacion de reservas rechace solicitudes sin JWT.
     */
    @Test
    void debeRechazarCreacionDeReservaSinToken() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/reservas",
                new ReservaRequest(1L, 1L, "ABC123"),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Valida el flujo principal: USER crea reserva, cajon cambia a RESERVADO
     * y el mismo cajon no puede reservarse dos veces.
     */
    @Test
    void debeCrearReservaConUsuarioUserYBloquearDuplicado() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long cajonId = crearCajonActivoEnBaseDeDatos(estacionamientoId);
        registrarUsuario("Cliente", USER_EMAIL);
        String userToken = iniciarSesion(USER_EMAIL);

        ResponseEntity<String> crearResponse =
                crearReserva(userToken, estacionamientoId, cajonId, "ABC123");
        JsonNode crearBody = objectMapper.readTree(crearResponse.getBody());
        String codigoReserva = crearBody.path("data").path("codigo").asText();
        Long reservaId = crearBody.path("data").path("id").asLong();

        assertThat(crearResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(crearBody.path("status").asInt()).isEqualTo(201);
        assertThat(crearBody.path("transactionId").asText()).isNotBlank();
        assertThat(codigoReserva).startsWith("RSV-");
        assertThat(crearBody.path("data").path("estado").asText()).isEqualTo("CREADA");
        assertThat(crearBody.path("data").path("tiempoExpiracionMinutos").asInt()).isGreaterThan(0);
        assertThat(consultarEstadoCajonEnBaseDeDatos(cajonId)).isEqualTo("RESERVADO");
        assertThat(consultarEstadoReservaEnBaseDeDatos(reservaId)).isEqualTo("CREADA");

        ResponseEntity<String> duplicadoResponse =
                crearReserva(userToken, estacionamientoId, cajonId, "ABC123");

        assertThat(duplicadoResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    /**
     * Verifica que USER pueda consultar sus reservas de forma paginada.
     */
    @Test
    void debeConsultarMisReservasConUsuarioUser() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long cajonId = crearCajonActivoEnBaseDeDatos(estacionamientoId);
        registrarUsuario("Cliente", USER_EMAIL);
        String userToken = iniciarSesion(USER_EMAIL);

        crearReserva(userToken, estacionamientoId, cajonId, "ABC123");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/reservas/mis-reservas?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(userToken)),
                String.class
        );
        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.path("status").asInt()).isEqualTo(200);
        assertThat(body.path("data").path("content")).hasSize(1);
        assertThat(body.path("data").path("content").get(0).path("codigo").asText())
                .startsWith("RSV-");
    }

    /**
     * Verifica que OPERADOR pueda consultar una reserva por codigo cuando el cliente llega.
     */
    @Test
    void debeConsultarReservaPorCodigoConOperador() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long cajonId = crearCajonActivoEnBaseDeDatos(estacionamientoId);
        registrarUsuario("Cliente", USER_EMAIL);
        String userToken = iniciarSesion(USER_EMAIL);
        ResponseEntity<String> crearResponse =
                crearReserva(userToken, estacionamientoId, cajonId, "ABC123");
        String codigoReserva = objectMapper.readTree(crearResponse.getBody())
                .path("data")
                .path("codigo")
                .asText();

        Long operadorId = registrarUsuario("Operador", OPERADOR_EMAIL);
        asignarRol(operadorId, "OPERADOR");
        String operadorToken = iniciarSesion(OPERADOR_EMAIL);

        ResponseEntity<String> consultarResponse = restTemplate.exchange(
                "/api/v1/reservas/codigo/" + codigoReserva,
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(operadorToken)),
                String.class
        );
        JsonNode consultarBody = objectMapper.readTree(consultarResponse.getBody());

        assertThat(consultarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultarBody.path("data").path("codigo").asText()).isEqualTo(codigoReserva);
        assertThat(consultarBody.path("data").path("estado").asText()).isEqualTo("CREADA");
    }

    /**
     * Verifica que ADMIN pueda consultar una reserva por su identificador interno.
     */
    @Test
    void debeConsultarReservaPorIdConAdmin() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long cajonId = crearCajonActivoEnBaseDeDatos(estacionamientoId);
        registrarUsuario("Cliente", USER_EMAIL);
        String userToken = iniciarSesion(USER_EMAIL);
        ResponseEntity<String> crearResponse =
                crearReserva(userToken, estacionamientoId, cajonId, "ABC123");
        Long reservaId = objectMapper.readTree(crearResponse.getBody())
                .path("data")
                .path("id")
                .asLong();

        Long adminId = registrarUsuario("Administrador", ADMIN_EMAIL);
        asignarRol(adminId, "ADMIN");
        String adminToken = iniciarSesion(ADMIN_EMAIL);

        ResponseEntity<String> consultarResponse = restTemplate.exchange(
                "/api/v1/reservas/" + reservaId,
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(adminToken)),
                String.class
        );
        JsonNode consultarBody = objectMapper.readTree(consultarResponse.getBody());

        assertThat(consultarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultarBody.path("data").path("id").asLong()).isEqualTo(reservaId);
    }

    /**
     * Verifica que USER pueda cancelar una reserva propia y que el cajon regrese a LIBRE.
     */
    @Test
    void debeCancelarReservaPropiaConUsuarioUser() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long cajonId = crearCajonActivoEnBaseDeDatos(estacionamientoId);
        registrarUsuario("Cliente", USER_EMAIL);
        String userToken = iniciarSesion(USER_EMAIL);
        ResponseEntity<String> crearResponse =
                crearReserva(userToken, estacionamientoId, cajonId, "ABC123");
        Long reservaId = objectMapper.readTree(crearResponse.getBody())
                .path("data")
                .path("id")
                .asLong();

        ResponseEntity<String> cancelarResponse = restTemplate.exchange(
                "/api/v1/reservas/" + reservaId + "/cancelar",
                HttpMethod.PATCH,
                new HttpEntity<>(crearHeadersConJwt(userToken)),
                String.class
        );
        JsonNode cancelarBody = objectMapper.readTree(cancelarResponse.getBody());

        assertThat(cancelarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelarBody.path("data").path("estado").asText()).isEqualTo("CANCELADA");
        assertThat(consultarEstadoReservaEnBaseDeDatos(reservaId)).isEqualTo("CANCELADA");
        assertThat(consultarEstadoCajonEnBaseDeDatos(cajonId)).isEqualTo("LIBRE");
    }

    /**
     * Verifica que la expiracion automatica marque reservas vencidas como EXPIRADA y libere cajones.
     *
     * <p>La prueba ejecuta directamente el service usado por el scheduler para evitar depender
     * del tiempo real de ejecucion de tareas programadas.</p>
     */
    @Test
    void debeExpirarReservasVencidasYLiberarCajones() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long cajonId = crearCajonActivoReservadoEnBaseDeDatos(estacionamientoId);
        Long usuarioId = registrarUsuario("Cliente", USER_EMAIL);
        Long reservaId = crearReservaVencidaEnBaseDeDatos(usuarioId, estacionamientoId, cajonId);

        int totalExpiradas = reservaService.expirarReservasVencidas();

        assertThat(totalExpiradas).isEqualTo(1);
        assertThat(consultarEstadoReservaEnBaseDeDatos(reservaId)).isEqualTo("EXPIRADA");
        assertThat(consultarEstadoCajonEnBaseDeDatos(cajonId)).isEqualTo("LIBRE");
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

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/usuarios",
                new HttpEntity<>(request, crearHeadersJson()),
                String.class
        );
        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return body.path("data").path("id").asLong();
    }

    /**
     * Asigna el rol indicado directamente para preparar escenarios de autorizacion.
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

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(request, crearHeadersJson()),
                String.class
        );
        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return body.path("accessToken").asText();
    }

    /**
     * Crea una reserva consumiendo el endpoint real con autenticacion Bearer.
     */
    private ResponseEntity<String> crearReserva(
            String accessToken,
            Long estacionamientoId,
            Long cajonId,
            String placa
    ) {
        ReservaRequest request = new ReservaRequest(estacionamientoId, cajonId, placa);

        return restTemplate.postForEntity(
                "/api/v1/reservas",
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
    }

    /**
     * Crea un estacionamiento activo directamente en la base para usarlo como dependencia de la reserva.
     */
    private Long crearEstacionamientoActivoEnBaseDeDatos() {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO estacionamiento (nombre, descripcion, latitud, longitud, activo)
                VALUES ('Estacionamiento Reserva Integracion', 'Dato de apoyo para pruebas', 19.43260800, -99.13320900, TRUE)
                RETURNING id
                """,
                Long.class
        );
    }

    /**
     * Crea un cajon LIBRE directamente en la base para que pueda ser reservado.
     */
    private Long crearCajonActivoEnBaseDeDatos(Long estacionamientoId) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO cajon (estacionamiento_id, numero, tipo, estado, activo)
                VALUES (?, 'R-01', 'AUTO', 'LIBRE', TRUE)
                RETURNING id
                """,
                Long.class,
                estacionamientoId
        );
    }

    /**
     * Crea un cajon RESERVADO directamente en la base para probar expiracion automatica.
     */
    private Long crearCajonActivoReservadoEnBaseDeDatos(Long estacionamientoId) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO cajon (estacionamiento_id, numero, tipo, estado, activo)
                VALUES (?, 'R-02', 'AUTO', 'RESERVADO', TRUE)
                RETURNING id
                """,
                Long.class,
                estacionamientoId
        );
    }

    /**
     * Crea una reserva vencida directamente en la base para validar la expiracion automatica.
     */
    private Long crearReservaVencidaEnBaseDeDatos(
            Long usuarioId,
            Long estacionamientoId,
            Long cajonId
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO reserva (
                    codigo,
                    placa,
                    estado,
                    fecha_reserva,
                    fecha_expiracion,
                    tiempo_expiracion_minutos,
                    usuario_id,
                    estacionamiento_id,
                    cajon_id,
                    activo,
                    fecha_creacion
                )
                VALUES (
                    'RSV-VENCIDA',
                    'ABC123',
                    'CREADA',
                    CURRENT_TIMESTAMP - INTERVAL '30 minutes',
                    CURRENT_TIMESTAMP - INTERVAL '10 minutes',
                    20,
                    ?,
                    ?,
                    ?,
                    TRUE,
                    CURRENT_TIMESTAMP - INTERVAL '30 minutes'
                )
                RETURNING id
                """,
                Long.class,
                usuarioId,
                estacionamientoId,
                cajonId
        );
    }

    /**
     * Consulta directamente el estado del cajon para confirmar el cambio a RESERVADO.
     */
    private String consultarEstadoCajonEnBaseDeDatos(Long cajonId) {
        return jdbcTemplate.queryForObject(
                "SELECT estado FROM cajon WHERE id = ?",
                String.class,
                cajonId
        );
    }

    /**
     * Consulta directamente el estado de la reserva para confirmar su persistencia.
     */
    private String consultarEstadoReservaEnBaseDeDatos(Long reservaId) {
        return jdbcTemplate.queryForObject(
                "SELECT estado FROM reserva WHERE id = ?",
                String.class,
                reservaId
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
     * Crea headers HTTP con Content-Type JSON.
     */
    private HttpHeaders crearHeadersJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Crea headers HTTP con Content-Type JSON y el JWT en formato Bearer.
     */
    private HttpHeaders crearHeadersConJwt(String accessToken) {
        HttpHeaders headers = crearHeadersJson();
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
