package com.kasaca.parkio.tarifa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.GlobalExceptionHandler;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoRequest;
import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoResponse;
import com.kasaca.parkio.tarifa.service.TarifaEstacionamientoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TarifaEstacionamientoControllerTest {

    @Mock
    private TarifaEstacionamientoService tarifaEstacionamientoService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    /**
     * Configura MockMvc en modo standalone para probar el controller sin levantar todo Spring.
     */
    @BeforeEach
    void setUp() {
        TarifaEstacionamientoController controller =
                new TarifaEstacionamientoController(tarifaEstacionamientoService);

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
     * Verifica que el endpoint GET devuelva la tarifa activa usando respuesta estandarizada.
     */
    @Test
    void debeConsultarTarifaPorEstacionamiento() throws Exception {
        TarifaEstacionamientoResponse response = crearResponse();

        when(tarifaEstacionamientoService.getTarifaByEstacionamientoId(1L, null))
                .thenReturn(response);

        mockMvc.perform(get("/tarifas/estacionamiento/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Tarifa consultada correctamente"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(3L))
                .andExpect(jsonPath("$.data.estacionamientoId").value(1L))
                .andExpect(jsonPath("$.data.precioPorHora").value(25.00))
                .andExpect(jsonPath("$.data.minutosTolerancia").value(10))
                .andExpect(jsonPath("$.data.cobrarFraccion").value(true))
                .andExpect(jsonPath("$.data.tarifaMinima").value(15.00));

        verify(tarifaEstacionamientoService)
                .getTarifaByEstacionamientoId(1L, null);
    }

    /**
     * Verifica que el endpoint POST cree una tarifa y responda HTTP 201.
     */
    @Test
    void debeCrearTarifa() throws Exception {
        TarifaEstacionamientoRequest request = crearRequest();
        TarifaEstacionamientoResponse response = crearResponse();

        when(tarifaEstacionamientoService.addTarifa(
                any(TarifaEstacionamientoRequest.class),
                nullable(Jwt.class)
        )).thenReturn(response);

        mockMvc.perform(post("/tarifas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Tarifa creada correctamente"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(3L))
                .andExpect(jsonPath("$.data.estacionamientoId").value(1L));

        verify(tarifaEstacionamientoService)
                .addTarifa(any(TarifaEstacionamientoRequest.class), nullable(Jwt.class));
    }

    /**
     * Verifica que el endpoint POST rechace requests que violan Jakarta Validation.
     */
    @Test
    void debeRechazarSolicitudInvalida() throws Exception {
        mockMvc.perform(post("/tarifas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estacionamientoId": 0,
                                  "precioPorHora": -1,
                                  "minutosTolerancia": -5,
                                  "cobrarFraccion": null,
                                  "tarifaMinima": -10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("La solicitud contiene datos inválidos"))
                .andExpect(jsonPath("$.validationErrors.estacionamientoId")
                        .value("El identificador del estacionamiento debe ser mayor que cero"))
                .andExpect(jsonPath("$.validationErrors.precioPorHora")
                        .value("El precio por hora no puede ser negativo"))
                .andExpect(jsonPath("$.validationErrors.minutosTolerancia")
                        .value("Los minutos de tolerancia no pueden ser negativos"))
                .andExpect(jsonPath("$.validationErrors.cobrarFraccion")
                        .value("El indicador de cobro por fraccion es obligatorio"))
                .andExpect(jsonPath("$.validationErrors.tarifaMinima")
                        .value("La tarifa minima no puede ser negativa"));

        verifyNoInteractions(tarifaEstacionamientoService);
    }

    /**
     * Verifica que el endpoint PUT actualice la tarifa de un estacionamiento.
     */
    @Test
    void debeActualizarTarifa() throws Exception {
        TarifaEstacionamientoRequest request = new TarifaEstacionamientoRequest(
                1L,
                new BigDecimal("30.00"),
                15,
                false,
                new BigDecimal("20.00")
        );
        TarifaEstacionamientoResponse response = new TarifaEstacionamientoResponse(
                3L,
                1L,
                request.precioPorHora(),
                request.minutosTolerancia(),
                request.cobrarFraccion(),
                request.tarifaMinima(),
                true,
                LocalDateTime.of(2026, 8, 1, 12, 0)
        );

        when(tarifaEstacionamientoService.updateTarifa(
                any(Long.class),
                any(TarifaEstacionamientoRequest.class),
                nullable(Jwt.class)
        )).thenReturn(response);

        mockMvc.perform(put("/tarifas/estacionamiento/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Tarifa actualizada correctamente"))
                .andExpect(jsonPath("$.data.precioPorHora").value(30.00))
                .andExpect(jsonPath("$.data.minutosTolerancia").value(15))
                .andExpect(jsonPath("$.data.cobrarFraccion").value(false))
                .andExpect(jsonPath("$.data.tarifaMinima").value(20.00));

        verify(tarifaEstacionamientoService)
                .updateTarifa(any(Long.class), any(TarifaEstacionamientoRequest.class), nullable(Jwt.class));
    }

    /**
     * Verifica que un conflicto de negocio se traduzca a HTTP 409.
     */
    @Test
    void debeResponderConflictCuandoYaExisteTarifaActiva() throws Exception {
        TarifaEstacionamientoRequest request = crearRequest();

        when(tarifaEstacionamientoService.addTarifa(
                any(TarifaEstacionamientoRequest.class),
                nullable(Jwt.class)
        )).thenThrow(new ConflictException("El estacionamiento con identificador '1' ya tiene una tarifa activa"));

        mockMvc.perform(post("/tarifas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("El estacionamiento con identificador '1' ya tiene una tarifa activa"));
    }

    /**
     * Verifica que una tarifa inexistente se traduzca a HTTP 404.
     */
    @Test
    void debeResponderNotFoundCuandoNoExisteTarifa() throws Exception {
        when(tarifaEstacionamientoService.getTarifaByEstacionamientoId(99L, null))
                .thenThrow(new ResourceNotFoundException("Tarifa del estacionamiento", 99L));

        mockMvc.perform(get("/tarifas/estacionamiento/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Tarifa del estacionamiento con identificador '99' no fue encontrado"));
    }

    /**
     * Verifica que el endpoint DELETE responda 204 y no devuelva cuerpo.
     */
    @Test
    void debeEliminarTarifa() throws Exception {
        mockMvc.perform(delete("/tarifas/estacionamiento/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(tarifaEstacionamientoService).deleteTarifa(1L, null);
    }

    /**
     * Construye un request valido para crear tarifas.
     */
    private TarifaEstacionamientoRequest crearRequest() {
        return new TarifaEstacionamientoRequest(
                1L,
                new BigDecimal("25.00"),
                10,
                true,
                new BigDecimal("15.00")
        );
    }

    /**
     * Construye el response esperado para una tarifa activa.
     */
    private TarifaEstacionamientoResponse crearResponse() {
        return new TarifaEstacionamientoResponse(
                3L,
                1L,
                new BigDecimal("25.00"),
                10,
                true,
                new BigDecimal("15.00"),
                true,
                LocalDateTime.of(2026, 8, 1, 12, 0)
        );
    }
}
