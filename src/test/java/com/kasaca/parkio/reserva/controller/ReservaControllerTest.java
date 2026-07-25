package com.kasaca.parkio.reserva.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kasaca.parkio.reserva.dto.ReservaRequest;
import com.kasaca.parkio.reserva.dto.ReservaResponse;
import com.kasaca.parkio.reserva.entity.EstadoReserva;
import com.kasaca.parkio.reserva.service.ReservaService;
import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.GlobalExceptionHandler;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

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
class ReservaControllerTest {

    @Mock
    private ReservaService reservaService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    /**
     * Configura MockMvc con el controlador de reservas, paginacion y manejo global de errores.
     */
    @BeforeEach
    void setUp() {
        ReservaController controller = new ReservaController(reservaService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver()
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    /**
     * Verifica que el endpoint cree una reserva y responda con HTTP 201.
     */
    @Test
    void debeCrearReserva() throws Exception {
        Jwt jwt = crearJwtUser();
        ReservaRequest request = crearRequest();
        ReservaResponse response = crearResponse();

        when(reservaService.crearReserva(eq(1L), any(ReservaRequest.class)))
                .thenReturn(response);

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(post("/reservas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.message").value("Reserva creada correctamente"))
                    .andExpect(jsonPath("$.transactionId").isNotEmpty())
                    .andExpect(jsonPath("$.data.id").value(30L))
                    .andExpect(jsonPath("$.data.codigo").value("RSV-ABC12345"))
                    .andExpect(jsonPath("$.data.estado").value("CREADA"))
                    .andExpect(jsonPath("$.data.tiempoExpiracionMinutos").value(20));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(reservaService).crearReserva(eq(1L), any(ReservaRequest.class));
    }

    /**
     * Verifica que Jakarta Validation rechace datos invalidos antes de llamar al service.
     */
    @Test
    void debeRechazarCreacionConDatosInvalidos() throws Exception {
        mockMvc.perform(post("/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estacionamientoId": 0,
                                  "cajonId": null,
                                  "placa": "PLACA-DEMASIADO-LARGA"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.estacionamientoId").value(
                        "El identificador del estacionamiento debe ser mayor que cero"
                ))
                .andExpect(jsonPath("$.validationErrors.cajonId").value(
                        "El cajon es obligatorio"
                ))
                .andExpect(jsonPath("$.validationErrors.placa").value(
                        "La placa no puede exceder 15 caracteres"
                ));

        verifyNoInteractions(reservaService);
    }

    /**
     * Verifica que el controller traduzca un conflicto de negocio a HTTP 409.
     */
    @Test
    void debeResponderConflictCuandoCajonNoEstaDisponible() throws Exception {
        Jwt jwt = crearJwtUser();

        when(reservaService.crearReserva(eq(1L), any(ReservaRequest.class)))
                .thenThrow(new ConflictException("El cajon no esta disponible para reservar."));

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(post("/reservas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value(
                            "El cajon no esta disponible para reservar."
                    ));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Verifica que el usuario autenticado pueda consultar sus reservas paginadas.
     */
    @Test
    void debeConsultarMisReservas() throws Exception {
        Jwt jwt = crearJwtUser();
        PageResponse<ReservaResponse> pageResponse = PageResponse.from(
                new PageImpl<>(List.of(crearResponse()), PageRequest.of(0, 10), 1)
        );

        when(reservaService.getReservasByUsuario(eq(1L), any()))
                .thenReturn(pageResponse);

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(get("/reservas/mis-reservas")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Reservas consultadas correctamente"))
                    .andExpect(jsonPath("$.data.content[0].codigo").value("RSV-ABC12345"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(reservaService).getReservasByUsuario(eq(1L), any());
    }

    /**
     * Verifica que una reserva pueda consultarse por su codigo publico.
     */
    @Test
    void debeConsultarReservaPorCodigo() throws Exception {
        when(reservaService.getReservaByCodigo("RSV-ABC12345"))
                .thenReturn(crearResponse());

        mockMvc.perform(get("/reservas/codigo/RSV-ABC12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Reserva consultada correctamente"))
                .andExpect(jsonPath("$.data.codigo").value("RSV-ABC12345"));

        verify(reservaService).getReservaByCodigo("RSV-ABC12345");
    }

    /**
     * Verifica que una consulta por codigo inexistente responda HTTP 404.
     */
    @Test
    void debeResponderNotFoundCuandoCodigoNoExiste() throws Exception {
        when(reservaService.getReservaByCodigo("RSV-NOEXISTE"))
                .thenThrow(new ResourceNotFoundException("Reserva", "RSV-NOEXISTE"));

        mockMvc.perform(get("/reservas/codigo/RSV-NOEXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(
                        "Reserva con identificador 'RSV-NOEXISTE' no fue encontrado"
                ));
    }

    /**
     * Verifica que ADMIN pueda consultar una reserva por su identificador interno.
     */
    @Test
    void debeConsultarReservaPorId() throws Exception {
        when(reservaService.getReservaById(30L)).thenReturn(crearResponse());

        mockMvc.perform(get("/reservas/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Reserva consultada correctamente"))
                .andExpect(jsonPath("$.data.id").value(30L));

        verify(reservaService).getReservaById(30L);
    }

    /**
     * Verifica que el usuario autenticado pueda cancelar una reserva propia.
     */
    @Test
    void debeCancelarReserva() throws Exception {
        Jwt jwt = crearJwtUser();
        ReservaResponse response = new ReservaResponse(
                30L,
                "RSV-ABC12345",
                "ABC123",
                EstadoReserva.CANCELADA,
                LocalDateTime.of(2026, 7, 25, 10, 0),
                LocalDateTime.of(2026, 7, 25, 10, 20),
                20,
                1L,
                10L,
                20L,
                true,
                LocalDateTime.of(2026, 7, 25, 10, 0)
        );

        when(reservaService.cancelarReserva(30L, 1L)).thenReturn(response);

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(patch("/reservas/30/cancelar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Reserva cancelada correctamente"))
                    .andExpect(jsonPath("$.transactionId").isNotEmpty())
                    .andExpect(jsonPath("$.data.id").value(30L))
                    .andExpect(jsonPath("$.data.estado").value("CANCELADA"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(reservaService).cancelarReserva(30L, 1L);
    }

    /**
     * Verifica que el controller traduzca una cancelacion no permitida a HTTP 409.
     */
    @Test
    void debeResponderConflictCuandoReservaNoPuedeCancelarse() throws Exception {
        Jwt jwt = crearJwtUser();

        when(reservaService.cancelarReserva(30L, 1L))
                .thenThrow(new ConflictException("Solo se pueden cancelar reservas en estado CREADA."));

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(patch("/reservas/30/cancelar"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value(
                            "Solo se pueden cancelar reservas en estado CREADA."
                    ));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Crea el request valido de reserva usado por los endpoints.
     */
    private ReservaRequest crearRequest() {
        return new ReservaRequest(10L, 20L, "ABC123");
    }

    /**
     * Crea el response esperado de reserva.
     */
    private ReservaResponse crearResponse() {
        return new ReservaResponse(
                30L,
                "RSV-ABC12345",
                "ABC123",
                EstadoReserva.CREADA,
                LocalDateTime.of(2026, 7, 25, 10, 0),
                LocalDateTime.of(2026, 7, 25, 10, 20),
                20,
                1L,
                10L,
                20L,
                true,
                LocalDateTime.of(2026, 7, 25, 10, 0)
        );
    }

    /**
     * Crea un JWT con usuarioId para simular al cliente autenticado en pruebas unitarias.
     */
    private Jwt crearJwtUser() {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("usuarioId", 1L)
                .claim("roles", List.of("USER"))
                .build();
    }
}
