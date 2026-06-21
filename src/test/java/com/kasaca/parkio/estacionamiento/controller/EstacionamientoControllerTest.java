package com.kasaca.parkio.estacionamiento.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoResponse;
import com.kasaca.parkio.estacionamiento.service.EstacionamientoService;
import com.kasaca.parkio.shared.exception.GlobalExceptionHandler;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
class EstacionamientoControllerTest {

    @Mock
    private EstacionamientoService estacionamientoService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        EstacionamientoController controller =
                new EstacionamientoController(estacionamientoService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @Test
    void debeListarEstacionamientos() throws Exception {
        EstacionamientoResponse response = crearResponse();

        when(estacionamientoService.getEstacionamientos())
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/estacionamientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nombre")
                        .value("Parkio Centro"))
                .andExpect(jsonPath("$[0].activo").value(true));

        verify(estacionamientoService).getEstacionamientos();
    }

    @Test
    void debeObtenerEstacionamientoPorId() throws Exception {
        EstacionamientoResponse response = crearResponse();

        when(estacionamientoService.getEstacionamientoById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/estacionamientos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre")
                        .value("Parkio Centro"))
                .andExpect(jsonPath("$.latitud")
                        .value(19.432608))
                .andExpect(jsonPath("$.longitud")
                        .value(-99.133209));

        verify(estacionamientoService)
                .getEstacionamientoById(1L);
    }

    @Test
    void debeResponderNotFoundCuandoNoExiste() throws Exception {
        when(estacionamientoService.getEstacionamientoById(99L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Estacionamiento",
                                99L
                        )
                );

        mockMvc.perform(get("/api/estacionamientos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "Estacionamiento con identificador '99' no fue encontrado"
                ))
                .andExpect(jsonPath("$.path")
                        .value("/api/estacionamientos/99"));
    }

    @Test
    void debeCrearEstacionamiento() throws Exception {
        EstacionamientoRequest request = crearRequest();
        EstacionamientoResponse response = crearResponse();

        when(estacionamientoService.addEstacionamiento(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/estacionamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre")
                        .value("Parkio Centro"))
                .andExpect(jsonPath("$.activo").value(true));

        verify(estacionamientoService)
                .addEstacionamiento(any(EstacionamientoRequest.class));
    }

    @Test
    void debeRechazarSolicitudInvalida() throws Exception {
        mockMvc.perform(post("/api/estacionamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "",
                                  "descripcion": "Prueba",
                                  "latitud": null,
                                  "longitud": 181
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "La solicitud contiene datos inválidos"
                ))
                .andExpect(jsonPath("$.validationErrors.nombre")
                        .value(
                                "El nombre del estacionamiento es obligatorio"
                        ))
                .andExpect(jsonPath("$.validationErrors.latitud")
                        .value("La latitud es obligatoria"))
                .andExpect(jsonPath("$.validationErrors.longitud")
                        .value(
                                "La longitud debe ser menor o igual a 180"
                        ));

        verifyNoInteractions(estacionamientoService);
    }

    @Test
    void debeRechazarJsonMalFormado() throws Exception {
        mockMvc.perform(post("/api/estacionamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Parkio Centro",
                                  "latitud":
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "El cuerpo de la solicitud no es válido"
                ));

        verifyNoInteractions(estacionamientoService);
    }

    @Test
    void debeActualizarEstacionamiento() throws Exception {
        EstacionamientoRequest request = new EstacionamientoRequest(
                "Parkio Reforma",
                "Sucursal Reforma",
                new BigDecimal("19.42700000"),
                new BigDecimal("-99.16770000")
        );

        EstacionamientoResponse response =
                new EstacionamientoResponse(
                        1L,
                        request.nombre(),
                        request.descripcion(),
                        request.latitud(),
                        request.longitud(),
                        true,
                        LocalDateTime.of(2026, 6, 21, 12, 0)
                );

        when(estacionamientoService.updateEstacionamiento(
                any(Long.class),
                any(EstacionamientoRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put("/api/estacionamientos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre")
                        .value("Parkio Reforma"))
                .andExpect(jsonPath("$.activo").value(true));

        verify(estacionamientoService)
                .updateEstacionamiento(
                        any(Long.class),
                        any(EstacionamientoRequest.class)
                );
    }

    @Test
    void debeEliminarEstacionamiento() throws Exception {
        mockMvc.perform(delete("/api/estacionamientos/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(estacionamientoService)
                .deleteEstacionamiento(1L);
    }

    private EstacionamientoRequest crearRequest() {
        return new EstacionamientoRequest(
                "Parkio Centro",
                "Sucursal Centro Histórico",
                new BigDecimal("19.43260800"),
                new BigDecimal("-99.13320900")
        );
    }

    private EstacionamientoResponse crearResponse() {
        return new EstacionamientoResponse(
                1L,
                "Parkio Centro",
                "Sucursal Centro Histórico",
                new BigDecimal("19.43260800"),
                new BigDecimal("-99.13320900"),
                true,
                LocalDateTime.of(2026, 6, 21, 12, 0)
        );
    }
}
