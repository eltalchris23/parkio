package com.kasaca.parkio.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.reserva.dto.ReservaRequest;
import com.kasaca.parkio.ticket.dto.TicketEntradaRequest;
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
 * Pruebas de integracion para validar el modulo Ticket con la aplicacion completa.
 *
 * <p>Estas pruebas usan PostgreSQL con perfil test, consumen endpoints HTTP reales,
 * validan seguridad JWT y comprueban los cambios persistidos en reserva, cajon y ticket.</p>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TicketIntegrationTest {

    private static final String TEST_DATABASE_NAME = "parkio_test";
    private static final String USER_EMAIL = "integration.user.ticket@parkio.com";
    private static final String OPERADOR_EMAIL = "integration.operador.ticket@parkio.com";
    private static final String OTRO_OPERADOR_EMAIL = "integration.otro.operador.ticket@parkio.com";
    private static final String PASSWORD = "clave-integracion";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Limpia datos variables y conserva roles base antes de cada escenario.
     *
     * <p>La validacion de base evita que un TRUNCATE accidental afecte parkio en lugar de parkio_test.</p>
     */
    @BeforeEach
    void limpiarDatosDePrueba() {
        validarBaseDeDatosDePrueba();

        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    usuario_rol,
                    usuario_estacionamiento,
                    ticket,
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
     * Verifica que el endpoint de entrada rechace solicitudes sin JWT.
     */
    @Test
    void debeRechazarRegistroDeEntradaSinToken() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/tickets/entrada",
                new TicketEntradaRequest("RSV-ABC12345"),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Verifica que un USER no pueda registrar entradas porque no tiene rol OPERADOR.
     */
    @Test
    void debeRechazarRegistroDeEntradaConRolUser() throws Exception {
        Long userId = registrarUsuario("Cliente", USER_EMAIL);
        String userToken = iniciarSesion(USER_EMAIL);
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long cajonId = crearCajonActivoEnBaseDeDatos(estacionamientoId);
        String codigoReserva = crearReservaYObtenerCodigo(userToken, estacionamientoId, cajonId, "ABC123");

        ResponseEntity<String> response = registrarEntrada(userToken, codigoReserva);

        assertThat(userId).isPositive();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Valida el flujo principal: OPERADOR convierte una reserva vigente en ticket abierto.
     */
    @Test
    void debeRegistrarEntradaConOperadorAsignado() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long cajonId = crearCajonActivoEnBaseDeDatos(estacionamientoId);
        registrarUsuario("Cliente", USER_EMAIL);
        String userToken = iniciarSesion(USER_EMAIL);
        String codigoReserva = crearReservaYObtenerCodigo(userToken, estacionamientoId, cajonId, "ABC123");
        Long reservaId = consultarReservaIdPorCodigo(codigoReserva);

        Long operadorId = registrarUsuario("Operador", OPERADOR_EMAIL);
        asignarRol(operadorId, "OPERADOR");
        asignarEstacionamiento(operadorId, estacionamientoId);
        String operadorToken = iniciarSesion(OPERADOR_EMAIL);

        ResponseEntity<String> response = registrarEntrada(operadorToken, codigoReserva);
        JsonNode body = objectMapper.readTree(response.getBody());
        Long ticketId = body.path("data").path("id").asLong();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(body.path("status").asInt()).isEqualTo(201);
        assertThat(body.path("transactionId").asText()).isNotBlank();
        assertThat(body.path("data").path("codigo").asText()).startsWith("TCK-");
        assertThat(body.path("data").path("estado").asText()).isEqualTo("ABIERTO");
        assertThat(body.path("data").path("reservaId").asLong()).isEqualTo(reservaId);
        assertThat(body.path("data").path("operadorEntradaId").asLong()).isEqualTo(operadorId);
        assertThat(consultarEstadoReservaEnBaseDeDatos(reservaId)).isEqualTo("USADA");
        assertThat(consultarEstadoCajonEnBaseDeDatos(cajonId)).isEqualTo("OCUPADO");
        assertThat(consultarEstadoTicketEnBaseDeDatos(ticketId)).isEqualTo("ABIERTO");
    }

    /**
     * Verifica que no se pueda convertir dos veces la misma reserva en ticket.
     */
    @Test
    void debeBloquearSegundoTicketParaLaMismaReserva() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long cajonId = crearCajonActivoEnBaseDeDatos(estacionamientoId);
        registrarUsuario("Cliente", USER_EMAIL);
        String userToken = iniciarSesion(USER_EMAIL);
        String codigoReserva = crearReservaYObtenerCodigo(userToken, estacionamientoId, cajonId, "ABC123");

        Long operadorId = registrarUsuario("Operador", OPERADOR_EMAIL);
        asignarRol(operadorId, "OPERADOR");
        asignarEstacionamiento(operadorId, estacionamientoId);
        String operadorToken = iniciarSesion(OPERADOR_EMAIL);

        ResponseEntity<String> primeraRespuesta = registrarEntrada(operadorToken, codigoReserva);
        ResponseEntity<String> segundaRespuesta = registrarEntrada(operadorToken, codigoReserva);

        assertThat(primeraRespuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(segundaRespuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    /**
     * Verifica que un operador de otro estacionamiento no pueda registrar la entrada.
     */
    @Test
    void debeRechazarOperadorNoAsignadoAlEstacionamiento() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long otroEstacionamientoId = crearOtroEstacionamientoActivoEnBaseDeDatos();
        Long cajonId = crearCajonActivoEnBaseDeDatos(estacionamientoId);
        registrarUsuario("Cliente", USER_EMAIL);
        String userToken = iniciarSesion(USER_EMAIL);
        String codigoReserva = crearReservaYObtenerCodigo(userToken, estacionamientoId, cajonId, "ABC123");

        Long operadorId = registrarUsuario("Otro Operador", OTRO_OPERADOR_EMAIL);
        asignarRol(operadorId, "OPERADOR");
        asignarEstacionamiento(operadorId, otroEstacionamientoId);
        String operadorToken = iniciarSesion(OTRO_OPERADOR_EMAIL);

        ResponseEntity<String> response = registrarEntrada(operadorToken, codigoReserva);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(consultarEstadoCajonEnBaseDeDatos(cajonId)).isEqualTo("RESERVADO");
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
     * Crea una reserva real y devuelve el codigo publico generado por el backend.
     */
    private String crearReservaYObtenerCodigo(
            String accessToken,
            Long estacionamientoId,
            Long cajonId,
            String placa
    ) throws Exception {
        ReservaRequest request = new ReservaRequest(estacionamientoId, cajonId, placa);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/reservas",
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return body.path("data").path("codigo").asText();
    }

    /**
     * Registra la entrada consumiendo el endpoint real de tickets.
     */
    private ResponseEntity<String> registrarEntrada(String accessToken, String codigoReserva) {
        TicketEntradaRequest request = new TicketEntradaRequest(codigoReserva);

        return restTemplate.postForEntity(
                "/api/v1/tickets/entrada",
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
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
     * Asigna un estacionamiento al operador directamente en la tabla intermedia.
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
     * Crea un estacionamiento activo directamente en base de datos.
     */
    private Long crearEstacionamientoActivoEnBaseDeDatos() {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO estacionamiento (nombre, descripcion, latitud, longitud, activo)
                VALUES ('Estacionamiento Ticket Integracion', 'Dato de apoyo para pruebas', 19.43260800, -99.13320900, TRUE)
                RETURNING id
                """,
                Long.class
        );
    }

    /**
     * Crea un segundo estacionamiento activo para probar asignaciones incorrectas.
     */
    private Long crearOtroEstacionamientoActivoEnBaseDeDatos() {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO estacionamiento (nombre, descripcion, latitud, longitud, activo)
                VALUES ('Otro Estacionamiento Ticket', 'Dato de apoyo para pruebas', 20.00000000, -100.00000000, TRUE)
                RETURNING id
                """,
                Long.class
        );
    }

    /**
     * Crea un cajon LIBRE directamente en base para que el usuario pueda reservarlo.
     */
    private Long crearCajonActivoEnBaseDeDatos(Long estacionamientoId) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO cajon (estacionamiento_id, numero, tipo, estado, activo)
                VALUES (?, 'T-01', 'AUTO', 'LIBRE', TRUE)
                RETURNING id
                """,
                Long.class,
                estacionamientoId
        );
    }

    /**
     * Consulta el id interno de una reserva a partir de su codigo publico.
     */
    private Long consultarReservaIdPorCodigo(String codigoReserva) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM reserva WHERE codigo = ?",
                Long.class,
                codigoReserva
        );
    }

    /**
     * Consulta directamente el estado de una reserva para validar cambios persistidos.
     */
    private String consultarEstadoReservaEnBaseDeDatos(Long reservaId) {
        return jdbcTemplate.queryForObject(
                "SELECT estado FROM reserva WHERE id = ?",
                String.class,
                reservaId
        );
    }

    /**
     * Consulta directamente el estado de un cajon para validar cambios persistidos.
     */
    private String consultarEstadoCajonEnBaseDeDatos(Long cajonId) {
        return jdbcTemplate.queryForObject(
                "SELECT estado FROM cajon WHERE id = ?",
                String.class,
                cajonId
        );
    }

    /**
     * Consulta directamente el estado de un ticket para validar su creacion.
     */
    private String consultarEstadoTicketEnBaseDeDatos(Long ticketId) {
        return jdbcTemplate.queryForObject(
                "SELECT estado FROM ticket WHERE id = ?",
                String.class,
                ticketId
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
     * Crea headers HTTP con Content-Type JSON y autenticacion Bearer.
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
