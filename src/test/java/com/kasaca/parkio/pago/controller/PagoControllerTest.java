package com.kasaca.parkio.pago.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kasaca.parkio.pago.dto.PagoRequest;
import com.kasaca.parkio.pago.dto.PagoResponse;
import com.kasaca.parkio.pago.entity.MetodoPago;
import com.kasaca.parkio.pago.service.PagoService;
import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.GlobalExceptionHandler;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PagoControllerTest {

    @Mock
    private PagoService pagoService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    /**
     * Configura MockMvc en modo standalone para probar el controller sin levantar todo Spring.
     */
    @BeforeEach
    void setUp() {
        PagoController controller = new PagoController(pagoService);

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
     * Verifica que el endpoint POST registre el pago y responda HTTP 201.
     */
    @Test
    void debeRegistrarPago() throws Exception {
        Jwt jwt = crearJwtOperador();
        PagoRequest request = crearRequest();
        PagoResponse response = crearResponse();

        when(pagoService.registrarPago(9L, request)).thenReturn(response);

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(post("/pagos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.message").value("Pago registrado correctamente"))
                    .andExpect(jsonPath("$.transactionId").isNotEmpty())
                    .andExpect(jsonPath("$.data.id").value(5L))
                    .andExpect(jsonPath("$.data.ticketId").value(3L))
                    .andExpect(jsonPath("$.data.montoTotal").value(72.50))
                    .andExpect(jsonPath("$.data.montoRecibido").value(100.00))
                    .andExpect(jsonPath("$.data.cambio").value(27.50))
                    .andExpect(jsonPath("$.data.metodoPago").value("EFECTIVO"))
                    .andExpect(jsonPath("$.data.estado").value("REGISTRADO"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(pagoService).registrarPago(9L, request);
    }

    /**
     * Verifica que el endpoint GET liste pagos paginados aplicando filtros opcionales.
     */
    @Test
    void debeListarPagos() throws Exception {
        Jwt jwt = crearJwtOperador();
        PagoResponse pago = crearResponse();
        PageResponse<PagoResponse> pageResponse = new PageResponse<>(
                List.of(pago),
                0,
                10,
                1,
                1,
                true,
                true,
                false
        );

        when(pagoService.getPagos(
                9L,
                1L,
                MetodoPago.EFECTIVO,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                PageRequest.of(0, 10)
        )).thenReturn(pageResponse);

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(get("/pagos")
                            .param("page", "0")
                            .param("size", "10")
                            .param("estacionamientoId", "1")
                            .param("metodoPago", "EFECTIVO")
                            .param("fechaInicio", "2026-08-01")
                            .param("fechaFin", "2026-08-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Pagos consultados correctamente"))
                    .andExpect(jsonPath("$.transactionId").isNotEmpty())
                    .andExpect(jsonPath("$.data.content[0].id").value(5L))
                    .andExpect(jsonPath("$.data.content[0].ticketId").value(3L))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(pagoService).getPagos(
                9L,
                1L,
                MetodoPago.EFECTIVO,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                PageRequest.of(0, 10)
        );
    }

    /**
     * Verifica que el endpoint POST aplique validaciones Jakarta Validation.
     */
    @Test
    void debeRechazarRequestInvalido() throws Exception {
        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketId": null,
                                  "montoRecibido": 0,
                                  "metodoPago": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.ticketId").value("El ticket es obligatorio"))
                .andExpect(jsonPath("$.validationErrors.montoRecibido").value("El monto recibido debe ser mayor a cero"))
                .andExpect(jsonPath("$.validationErrors.metodoPago").value("El metodo de pago es obligatorio"));

        verifyNoInteractions(pagoService);
    }

    /**
     * Verifica que un conflicto de negocio se traduzca a HTTP 409.
     */
    @Test
    void debeResponderConflictCuandoMontoEsInsuficiente() throws Exception {
        Jwt jwt = crearJwtOperador();

        when(pagoService.registrarPago(any(), any(PagoRequest.class)))
                .thenThrow(new ConflictException("El monto recibido es menor al monto total del ticket."));

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(post("/pagos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("El monto recibido es menor al monto total del ticket."));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Verifica que el endpoint GET consulte el pago asociado a un ticket.
     */
    @Test
    void debeConsultarPagoPorTicket() throws Exception {
        Jwt jwt = crearJwtOperador();
        PagoResponse response = crearResponse();

        when(pagoService.getPagoByTicketId(9L, 3L)).thenReturn(response);

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(get("/pagos/ticket/3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Pago consultado correctamente"))
                    .andExpect(jsonPath("$.data.id").value(5L))
                    .andExpect(jsonPath("$.data.ticketId").value(3L));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(pagoService).getPagoByTicketId(9L, 3L);
    }

    /**
     * Verifica que un pago inexistente se traduzca a HTTP 404.
     */
    @Test
    void debeResponderNotFoundCuandoNoExistePago() throws Exception {
        Jwt jwt = crearJwtOperador();

        when(pagoService.getPagoByTicketId(any(), any()))
                .thenThrow(new ResourceNotFoundException("Pago del ticket", 99L));

        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(get("/pagos/ticket/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message")
                            .value("Pago del ticket con identificador '99' no fue encontrado"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Construye un request valido para registrar pago.
     */
    private PagoRequest crearRequest() {
        return new PagoRequest(3L, new BigDecimal("100.00"), MetodoPago.EFECTIVO);
    }

    /**
     * Construye el response esperado para las pruebas del controller.
     */
    private PagoResponse crearResponse() {
        return new PagoResponse(
                5L,
                3L,
                "TCK-ABC12345",
                new BigDecimal("72.50"),
                new BigDecimal("100.00"),
                new BigDecimal("27.50"),
                "EFECTIVO",
                "REGISTRADO",
                LocalDateTime.of(2026, 8, 1, 18, 30),
                9L,
                true,
                LocalDateTime.of(2026, 8, 1, 18, 30)
        );
    }

    /**
     * Construye un JWT de operador con el claim usuarioId usado por el controller.
     */
    private Jwt crearJwtOperador() {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(claims -> claims.putAll(Map.of(
                        "usuarioId", 9L,
                        "roles", List.of("OPERADOR")
                )))
                .build();
    }
}
