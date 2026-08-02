package com.kasaca.parkio.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.pago.dto.PagoRequest;
import com.kasaca.parkio.pago.entity.MetodoPago;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integracion para validar el modulo Pago con la aplicacion completa.
 *
 * <p>Estas pruebas usan PostgreSQL con perfil test, consumen endpoints HTTP reales
 * y validan el flujo salida pendiente de pago -> pago registrado -> ticket cerrado -> cajon libre.</p>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PagoIntegrationTest {

    private static final String TEST_DATABASE_NAME = "parkio_test";
    private static final String USER_EMAIL = "integration.user.pago@parkio.com";
    private static final String OPERADOR_EMAIL = "integration.operador.pago@parkio.com";
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
     * <p>La validacion de base evita que el TRUNCATE afecte la base local parkio.</p>
     */
    @BeforeEach
    void limpiarDatosDePrueba() {
        validarBaseDeDatosDePrueba();

        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    usuario_rol,
                    usuario_estacionamiento,
                    pago,
                    ticket,
                    reserva,
                    tarifa_estacionamiento,
                    cajon,
                    estacionamiento,
                    usuario
                RESTART IDENTITY
                CASCADE
                """);

        asegurarRolesBase();
    }

    /**
     * Verifica que el endpoint de pagos rechace solicitudes sin JWT.
     */
    @Test
    void debeRechazarRegistroDePagoSinToken() {
        PagoRequest request = new PagoRequest(1L, new BigDecimal("100.00"), MetodoPago.EFECTIVO);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/pagos",
                new HttpEntity<>(request, crearHeadersJson()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Valida el flujo completo de pago: registra salida, cobra, calcula cambio,
     * cierra el ticket y libera el cajon.
     */
    @Test
    void debeRegistrarPagoYCerrarTicketLiberandoCajon() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long cajonId = crearCajonActivoEnBaseDeDatos(estacionamientoId);
        crearTarifaActivaEnBaseDeDatos(estacionamientoId);

        registrarUsuario("Cliente", USER_EMAIL);
        String userToken = iniciarSesion(USER_EMAIL);
        String codigoReserva = crearReservaYObtenerCodigo(userToken, estacionamientoId, cajonId);

        Long operadorId = registrarUsuario("Operador", OPERADOR_EMAIL);
        asignarRol(operadorId, "OPERADOR");
        asignarEstacionamiento(operadorId, estacionamientoId);
        String operadorToken = iniciarSesion(OPERADOR_EMAIL);

        Long ticketId = registrarEntradaYObtenerTicketId(operadorToken, codigoReserva);
        registrarSalida(operadorToken, ticketId);

        PagoRequest pagoRequest = new PagoRequest(ticketId, new BigDecimal("100.00"), MetodoPago.EFECTIVO);
        ResponseEntity<String> pagoResponse = restTemplate.postForEntity(
                "/api/v1/pagos",
                new HttpEntity<>(pagoRequest, crearHeadersConJwt(operadorToken)),
                String.class
        );
        JsonNode pagoBody = objectMapper.readTree(pagoResponse.getBody());

        assertThat(pagoResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(pagoBody.path("status").asInt()).isEqualTo(201);
        assertThat(pagoBody.path("message").asText()).isEqualTo("Pago registrado correctamente");
        assertThat(pagoBody.path("transactionId").asText()).isNotBlank();
        assertThat(pagoBody.path("data").path("ticketId").asLong()).isEqualTo(ticketId);
        assertThat(pagoBody.path("data").path("montoTotal").decimalValue()).isEqualByComparingTo("15.00");
        assertThat(pagoBody.path("data").path("montoRecibido").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(pagoBody.path("data").path("cambio").decimalValue()).isEqualByComparingTo("85.00");
        assertThat(pagoBody.path("data").path("metodoPago").asText()).isEqualTo("EFECTIVO");
        assertThat(pagoBody.path("data").path("estado").asText()).isEqualTo("REGISTRADO");
        assertThat(pagoBody.path("data").path("operadorId").asLong()).isEqualTo(operadorId);
        assertThat(consultarEstadoTicketEnBaseDeDatos(ticketId)).isEqualTo("CERRADO");
        assertThat(consultarEstadoCajonEnBaseDeDatos(cajonId)).isEqualTo("LIBRE");

        ResponseEntity<String> consultaPagoResponse = restTemplate.exchange(
                "/api/v1/pagos/ticket/" + ticketId,
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(userToken)),
                String.class
        );
        JsonNode consultaPagoBody = objectMapper.readTree(consultaPagoResponse.getBody());

        assertThat(consultaPagoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultaPagoBody.path("data").path("ticketId").asLong()).isEqualTo(ticketId);
        assertThat(consultaPagoBody.path("data").path("cambio").decimalValue()).isEqualByComparingTo("85.00");
    }

    /**
     * Valida que OPERADOR pueda listar pagos paginados usando filtros por estacionamiento,
     * metodo de pago y rango de fechas dentro de su alcance asignado.
     */
    @Test
    void debeListarPagosPaginadosConFiltros() throws Exception {
        FlujoPago flujo = prepararFlujoConPagoRegistrado();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/pagos?page=0&size=10&sort=fechaPago,desc"
                        + "&estacionamientoId=" + flujo.estacionamientoId()
                        + "&metodoPago=EFECTIVO"
                        + "&fechaInicio=2026-01-01"
                        + "&fechaFin=2026-12-31",
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(flujo.operadorToken())),
                String.class
        );
        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.path("status").asInt()).isEqualTo(200);
        assertThat(body.path("message").asText()).isEqualTo("Pagos consultados correctamente");
        assertThat(body.path("transactionId").asText()).isNotBlank();
        assertThat(body.path("data").path("content")).hasSize(1);
        assertThat(body.path("data").path("content").get(0).path("ticketId").asLong()).isEqualTo(flujo.ticketId());
        assertThat(body.path("data").path("content").get(0).path("metodoPago").asText()).isEqualTo("EFECTIVO");
        assertThat(body.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(body.path("data").path("page").asInt()).isZero();
        assertThat(body.path("data").path("size").asInt()).isEqualTo(10);
    }

    /**
     * Verifica que USER conserve la consulta por ticket, pero no pueda usar el listado general de pagos.
     */
    @Test
    void debeRechazarListadoGeneralDePagosConUser() throws Exception {
        FlujoPago flujo = prepararFlujoConPagoRegistrado();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/pagos?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(crearHeadersConJwt(flujo.userToken())),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Prepara el flujo completo necesario para tener un pago real consultable.
     *
     * <p>Crea estacionamiento, cajon, tarifa, cliente, reserva, operador, ticket,
     * salida pendiente de pago y finalmente el pago registrado.</p>
     */
    private FlujoPago prepararFlujoConPagoRegistrado() throws Exception {
        Long estacionamientoId = crearEstacionamientoActivoEnBaseDeDatos();
        Long cajonId = crearCajonActivoEnBaseDeDatos(estacionamientoId);
        crearTarifaActivaEnBaseDeDatos(estacionamientoId);

        registrarUsuario("Cliente", USER_EMAIL);
        String userToken = iniciarSesion(USER_EMAIL);
        String codigoReserva = crearReservaYObtenerCodigo(userToken, estacionamientoId, cajonId);

        Long operadorId = registrarUsuario("Operador", OPERADOR_EMAIL);
        asignarRol(operadorId, "OPERADOR");
        asignarEstacionamiento(operadorId, estacionamientoId);
        String operadorToken = iniciarSesion(OPERADOR_EMAIL);

        Long ticketId = registrarEntradaYObtenerTicketId(operadorToken, codigoReserva);
        registrarSalida(operadorToken, ticketId);
        registrarPago(operadorToken, ticketId);

        return new FlujoPago(estacionamientoId, ticketId, userToken, operadorToken);
    }

    /**
     * Registra un pago real para un ticket pendiente de pago.
     */
    private void registrarPago(String accessToken, Long ticketId) {
        PagoRequest pagoRequest = new PagoRequest(ticketId, new BigDecimal("100.00"), MetodoPago.EFECTIVO);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/pagos",
                new HttpEntity<>(pagoRequest, crearHeadersConJwt(accessToken)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    /**
     * Crea un usuario mediante el endpoint publico y devuelve su identificador.
     */
    private Long registrarUsuario(String nombre, String email) throws Exception {
        UsuarioCreateRequest request = new UsuarioCreateRequest(nombre, "Integracion", email, PASSWORD);

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
     * Inicia sesion y devuelve el JWT emitido por el backend.
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
     * Crea una reserva real y devuelve su codigo publico.
     */
    private String crearReservaYObtenerCodigo(String accessToken, Long estacionamientoId, Long cajonId) throws Exception {
        ReservaRequest request = new ReservaRequest(estacionamientoId, cajonId, "ABC123");

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
     * Registra la entrada y devuelve el identificador del ticket creado.
     */
    private Long registrarEntradaYObtenerTicketId(String accessToken, String codigoReserva) throws Exception {
        TicketEntradaRequest request = new TicketEntradaRequest(codigoReserva);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/tickets/entrada",
                new HttpEntity<>(request, crearHeadersConJwt(accessToken)),
                String.class
        );
        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return body.path("data").path("id").asLong();
    }

    /**
     * Registra la salida para dejar el ticket en PENDIENTE_PAGO.
     */
    private void registrarSalida(String accessToken, Long ticketId) {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/tickets/" + ticketId + "/salida",
                HttpMethod.PATCH,
                new HttpEntity<>(crearHeadersConJwt(accessToken)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultarEstadoTicketEnBaseDeDatos(ticketId)).isEqualTo("PENDIENTE_PAGO");
    }

    /**
     * Asigna un rol directamente para preparar escenarios de autorizacion.
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
     * Crea un estacionamiento activo directamente en la base de pruebas.
     */
    private Long crearEstacionamientoActivoEnBaseDeDatos() {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO estacionamiento (nombre, descripcion, latitud, longitud, activo)
                VALUES ('Estacionamiento Pago Integracion', 'Dato de apoyo para pruebas', 19.43260800, -99.13320900, TRUE)
                RETURNING id
                """,
                Long.class
        );
    }

    /**
     * Crea un cajon LIBRE directamente en la base de pruebas.
     */
    private Long crearCajonActivoEnBaseDeDatos(Long estacionamientoId) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO cajon (estacionamiento_id, numero, tipo, estado, activo)
                VALUES (?, 'P-01', 'AUTO', 'LIBRE', TRUE)
                RETURNING id
                """,
                Long.class,
                estacionamientoId
        );
    }

    /**
     * Crea una tarifa activa para que la salida pueda calcular el cobro.
     */
    private Long crearTarifaActivaEnBaseDeDatos(Long estacionamientoId) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO tarifa_estacionamiento (
                    estacionamiento_id,
                    precio_por_hora,
                    minutos_tolerancia,
                    cobrar_fraccion,
                    tarifa_minima,
                    activo
                )
                VALUES (?, 25.00, 10, TRUE, 15.00, TRUE)
                RETURNING id
                """,
                Long.class,
                estacionamientoId
        );
    }

    /**
     * Consulta directamente el estado del ticket para validar cambios persistidos.
     */
    private String consultarEstadoTicketEnBaseDeDatos(Long ticketId) {
        return jdbcTemplate.queryForObject("SELECT estado FROM ticket WHERE id = ?", String.class, ticketId);
    }

    /**
     * Consulta directamente el estado del cajon para validar que el pago lo libere.
     */
    private String consultarEstadoCajonEnBaseDeDatos(Long cajonId) {
        return jdbcTemplate.queryForObject("SELECT estado FROM cajon WHERE id = ?", String.class, cajonId);
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
        String databaseName = jdbcTemplate.queryForObject("SELECT current_database()", String.class);

        assertThat(databaseName).isEqualTo(TEST_DATABASE_NAME);
    }

    /**
     * Agrupa los datos principales creados durante el flujo de pago para reutilizarlos en pruebas.
     */
    private record FlujoPago(
            Long estacionamientoId,
            Long ticketId,
            String userToken,
            String operadorToken
    ) {
    }
}
