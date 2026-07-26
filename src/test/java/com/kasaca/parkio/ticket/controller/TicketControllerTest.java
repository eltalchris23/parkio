package com.kasaca.parkio.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
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
