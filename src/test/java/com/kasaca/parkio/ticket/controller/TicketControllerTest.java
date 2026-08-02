package com.kasaca.parkio.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.GlobalExceptionHandler;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.ticket.dto.TicketEntradaRequest;
import com.kasaca.parkio.ticket.dto.TicketResponse;
import com.kasaca.parkio.ticket.entity.EstadoTicket;
import com.kasaca.parkio.ticket.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    @Mock
    private TicketService ticketService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    /**
     * Configura MockMvc con el controller de tickets y el manejador global de excepciones.
     */
    @BeforeEach
    void setUp() {
        TicketController controller = new TicketController(ticketService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(),
                        new PageableHandlerMethodArgumentResolver()
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    /**
     * Verifica que el endpoint liste tickets paginados usando el usuario autenticado del JWT.
     */
    @Test
    void debeListarTickets() throws Exception {
        Jwt jwt = crearJwtOperador();
        TicketResponse ticket = crearResponse();
        PageResponse<TicketResponse> response = PageResponse.from(
                new PageImpl<>(List.of(ticket), PageRequest.of(0, 10), 1)
        );

        when(ticketService.getTickets(eq(2L), eq(null), eq(null), any())).thenReturn(response);

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(get("/tickets")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Tickets consultados correctamente"))
                    .andExpect(jsonPath("$.transactionId").isNotEmpty())
                    .andExpect(jsonPath("$.data.content[0].id").value(40L))
                    .andExpect(jsonPath("$.data.content[0].codigo").value("TCK-ABC12345"))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(ticketService).getTickets(eq(2L), eq(null), eq(null), any());
    }

    /**
     * Verifica que el endpoint envie al service los filtros opcionales de estado y estacionamiento.
     */
    @Test
    void debeListarTicketsConFiltros() throws Exception {
        Jwt jwt = crearJwtOperador();
        TicketResponse ticket = crearResponse();
        PageResponse<TicketResponse> response = PageResponse.from(
                new PageImpl<>(List.of(ticket), PageRequest.of(0, 10), 1)
        );

        when(ticketService.getTickets(eq(2L), eq(EstadoTicket.ABIERTO), eq(10L), any()))
                .thenReturn(response);

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(get("/tickets")
                            .param("estado", "ABIERTO")
                            .param("estacionamientoId", "10")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Tickets consultados correctamente"))
                    .andExpect(jsonPath("$.data.content[0].estado").value("ABIERTO"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(ticketService).getTickets(eq(2L), eq(EstadoTicket.ABIERTO), eq(10L), any());
    }

    /**
     * Verifica que el endpoint consulte un ticket por identificador.
     */
    @Test
    void debeConsultarTicketPorId() throws Exception {
        Jwt jwt = crearJwtOperador();
        TicketResponse response = crearResponse();

        when(ticketService.getTicketById(2L, 40L)).thenReturn(response);

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(get("/tickets/40"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Ticket consultado correctamente"))
                    .andExpect(jsonPath("$.data.id").value(40L))
                    .andExpect(jsonPath("$.data.codigo").value("TCK-ABC12345"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(ticketService).getTicketById(2L, 40L);
    }

    /**
     * Verifica que el manejador global responda 404 cuando el ticket consultado no existe.
     */
    @Test
    void debeResponderNotFoundCuandoTicketNoExisteAlConsultar() throws Exception {
        Jwt jwt = crearJwtOperador();

        when(ticketService.getTicketById(2L, 99L))
                .thenThrow(new ResourceNotFoundException("Ticket", 99L));

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(get("/tickets/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Ticket con identificador '99' no fue encontrado"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(ticketService).getTicketById(2L, 99L);
    }

    /**
     * Verifica que el endpoint registre la entrada y responda con HTTP 201.
     */
    @Test
    void debeRegistrarEntrada() throws Exception {
        Jwt jwt = crearJwtOperador();
        TicketEntradaRequest request = new TicketEntradaRequest("RSV-ABC12345");
        TicketResponse response = crearResponse();

        when(ticketService.registrarEntrada(eq(2L), any(TicketEntradaRequest.class)))
                .thenReturn(response);

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(post("/tickets/entrada")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.message").value("Ticket creado correctamente"))
                    .andExpect(jsonPath("$.transactionId").isNotEmpty())
                    .andExpect(jsonPath("$.data.id").value(40L))
                    .andExpect(jsonPath("$.data.codigo").value("TCK-ABC12345"))
                    .andExpect(jsonPath("$.data.estado").value("ABIERTO"))
                    .andExpect(jsonPath("$.data.reservaId").value(30L))
                    .andExpect(jsonPath("$.data.operadorEntradaId").value(2L));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(ticketService).registrarEntrada(eq(2L), any(TicketEntradaRequest.class));
    }

    /**
     * Verifica que Jakarta Validation rechace solicitudes sin codigo de reserva.
     */
    @Test
    void debeRechazarEntradaSinCodigoReserva() throws Exception {
        mockMvc.perform(post("/tickets/entrada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoReserva": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.codigoReserva").value(
                        "El codigo de reserva es obligatorio"
                ));

        verifyNoInteractions(ticketService);
    }

    /**
     * Verifica que el controller traduzca una reserva inexistente a HTTP 404.
     */
    @Test
    void debeResponderNotFoundCuandoReservaNoExiste() throws Exception {
        Jwt jwt = crearJwtOperador();

        when(ticketService.registrarEntrada(eq(2L), any(TicketEntradaRequest.class)))
                .thenThrow(new ResourceNotFoundException("Reserva", "RSV-NOEXISTE"));

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(post("/tickets/entrada")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new TicketEntradaRequest("RSV-NOEXISTE")
                            )))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value(
                            "Reserva con identificador 'RSV-NOEXISTE' no fue encontrado"
                    ));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Verifica que el controller traduzca conflictos de negocio a HTTP 409.
     */
    @Test
    void debeResponderConflictCuandoReservaNoPuedeConvertirse() throws Exception {
        Jwt jwt = crearJwtOperador();

        when(ticketService.registrarEntrada(eq(2L), any(TicketEntradaRequest.class)))
                .thenThrow(new ConflictException("La reserva ya expiro y no puede convertirse en ticket."));

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(post("/tickets/entrada")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new TicketEntradaRequest("RSV-ABC12345")
                            )))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value(
                            "La reserva ya expiro y no puede convertirse en ticket."
                    ));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Verifica que el endpoint registre la salida y responda con HTTP 200.
     */
    @Test
    void debeRegistrarSalida() throws Exception {
        Jwt jwt = crearJwtOperador();
        TicketResponse response = crearResponseCerrado();

        when(ticketService.registrarSalida(2L, 40L)).thenReturn(response);

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(patch("/tickets/40/salida"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Salida registrada correctamente"))
                    .andExpect(jsonPath("$.transactionId").isNotEmpty())
                    .andExpect(jsonPath("$.data.id").value(40L))
                    .andExpect(jsonPath("$.data.estado").value("PENDIENTE_PAGO"))
                    .andExpect(jsonPath("$.data.fechaSalida").isNotEmpty());
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(ticketService).registrarSalida(2L, 40L);
    }

    /**
     * Verifica que el controller traduzca un ticket inexistente a HTTP 404.
     */
    @Test
    void debeResponderNotFoundCuandoTicketNoExisteAlRegistrarSalida() throws Exception {
        Jwt jwt = crearJwtOperador();

        when(ticketService.registrarSalida(2L, 99L))
                .thenThrow(new ResourceNotFoundException("Ticket", 99L));

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(patch("/tickets/99/salida"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value(
                            "Ticket con identificador '99' no fue encontrado"
                    ));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Verifica que el controller traduzca conflictos de salida a HTTP 409.
     */
    @Test
    void debeResponderConflictCuandoTicketNoPuedeCerrarse() throws Exception {
        Jwt jwt = crearJwtOperador();

        when(ticketService.registrarSalida(2L, 40L))
                .thenThrow(new ConflictException(
                        "Solo se puede registrar salida para tickets en estado ABIERTO."
                ));

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(patch("/tickets/40/salida"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value(
                            "Solo se puede registrar salida para tickets en estado ABIERTO."
                    ));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Crea un JWT simulado con usuarioId de operador para pruebas de controller.
     */
    private Jwt crearJwtOperador() {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of(
                        "sub", "operador@parkio.com",
                        "usuarioId", 2L,
                        "roles", List.of("OPERADOR")
                )
        );
    }

    /**
     * Crea un response fijo para validar la estructura del ApiResponse.
     */
    private TicketResponse crearResponse() {
        return new TicketResponse(
                40L,
                "TCK-ABC12345",
                EstadoTicket.ABIERTO,
                "ABC123",
                LocalDateTime.of(2026, 7, 25, 10, 0),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                30L,
                1L,
                2L,
                10L,
                20L,
                true,
                LocalDateTime.of(2026, 7, 25, 10, 1)
        );
    }

    /**
     * Crea un response fijo para validar la salida cerrada del ticket.
     */
    private TicketResponse crearResponseCerrado() {
        return new TicketResponse(
                40L,
                "TCK-ABC12345",
                EstadoTicket.PENDIENTE_PAGO,
                "ABC123",
                LocalDateTime.of(2026, 7, 25, 10, 0),
                LocalDateTime.of(2026, 7, 25, 11, 0),
                60,
                new BigDecimal("25.00"),
                new BigDecimal("25.00"),
                10,
                true,
                new BigDecimal("15.00"),
                30L,
                1L,
                2L,
                10L,
                20L,
                true,
                LocalDateTime.of(2026, 7, 25, 10, 1)
        );
    }
}
