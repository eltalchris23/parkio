package com.kasaca.parkio.cajon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.dto.CajonEstadoRequest;
import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.entity.TipoCajon;
import com.kasaca.parkio.cajon.service.CajonService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CajonControllerTest {

    @Mock
    private CajonService cajonService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        CajonController controller = new CajonController(cajonService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @Test
    void debeListarTodosLosCajones() throws Exception {
        PageResponse<CajonResponse> pageResponse = PageResponse.from(
                new PageImpl<>(List.of(crearResponse()), PageRequest.of(0, 10), 1)
        );

        when(cajonService.getCajones(any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/cajones")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "numero,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Cajones consultados correctamente"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].numero").value("A-001"))
                .andExpect(jsonPath("$.data.content[0].tipo").value("AUTO"))
                .andExpect(jsonPath("$.data.content[0].estado").value("LIBRE"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.page").value(0));

        verify(cajonService).getCajones(any());
    }

    @Test
    void debeListarCajonesPorEstacionamiento() throws Exception {
        PageResponse<CajonResponse> pageResponse = PageResponse.from(
                new PageImpl<>(List.of(crearResponse()), PageRequest.of(0, 10), 1)
        );

        when(cajonService.getCajonesByEstacionamientoId(any(Long.class), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/cajones")
                        .param("estacionamientoId", "10")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[0].estacionamientoId")
                        .value(10L));

        verify(cajonService)
                .getCajonesByEstacionamientoId(any(Long.class), any());
    }

    @Test
    void debeObtenerCajonPorId() throws Exception {
        when(cajonService.getCajon(1L)).thenReturn(crearResponse());

        mockMvc.perform(get("/cajones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Cajon consultado correctamente"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.numero").value("A-001"));

        verify(cajonService).getCajon(1L);
    }

    @Test
    void debeResponderNotFoundCuandoCajonNoExiste() throws Exception {
        when(cajonService.getCajon(99L))
                .thenThrow(new ResourceNotFoundException("Cajón", 99L));

        mockMvc.perform(get("/cajones/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(
                        "Cajón con identificador '99' no fue encontrado"
                ));
    }

    @Test
    void debeCrearCajon() throws Exception {
        CajonRequest request = crearRequest();

        when(cajonService.addCajon(any(CajonRequest.class)))
                .thenReturn(crearResponse());

        mockMvc.perform(post("/cajones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Cajon creado correctamente"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.estado").value("LIBRE"));

        verify(cajonService).addCajon(any(CajonRequest.class));
    }

    @Test
    void debeResponderConflictCuandoNumeroEstaDuplicado() throws Exception {
        when(cajonService.addCajon(any(CajonRequest.class)))
                .thenThrow(new ConflictException(
                        "Ya existe el cajón 'A-001' en el estacionamiento '10'"
                ));

        mockMvc.perform(post("/cajones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                crearRequest()
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "Ya existe el cajón 'A-001' en el estacionamiento '10'"
                ));
    }

    @Test
    void debeRechazarSolicitudInvalida() throws Exception {
        mockMvc.perform(post("/cajones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "numero": "",
                                  "tipo": null,
                                  "estacionamientoId": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "La solicitud contiene datos inválidos"
                ))
                .andExpect(jsonPath("$.validationErrors.numero").value(
                        "El número del cajón es obligatorio"
                ))
                .andExpect(jsonPath("$.validationErrors.tipo").value(
                        "El tipo de cajón es obligatorio"
                ))
                .andExpect(jsonPath("$.validationErrors.estacionamientoId")
                        .value(
                                "El identificador del estacionamiento debe ser mayor que cero"
                        ));

        verifyNoInteractions(cajonService);
    }

    @Test
    void debeRechazarTipoDeCajonInvalido() throws Exception {
        mockMvc.perform(post("/cajones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "numero": "A-001",
                                  "tipo": "CAMION",
                                  "estacionamientoId": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "El cuerpo de la solicitud no es válido"
                ));

        verifyNoInteractions(cajonService);
    }

    @Test
    void debeActualizarCajon() throws Exception {
        CajonRequest request = new CajonRequest(
                "B-002",
                TipoCajon.ELECTRICO,
                10L
        );
        CajonResponse response = new CajonResponse(
                1L,
                "B-002",
                TipoCajon.ELECTRICO,
                EstadoCajon.LIBRE,
                10L,
                true,
                LocalDateTime.of(2026, 6, 27, 12, 0)
        );

        when(cajonService.updateCajon(
                any(Long.class),
                any(CajonRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put("/cajones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Cajon actualizado correctamente"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.numero").value("B-002"))
                .andExpect(jsonPath("$.data.tipo").value("ELECTRICO"));

        verify(cajonService).updateCajon(
                any(Long.class),
                any(CajonRequest.class)
        );
    }

    @Test
    void debeActualizarEstadoDelCajon() throws Exception {
        CajonEstadoRequest request = new CajonEstadoRequest(
                EstadoCajon.OCUPADO
        );
        CajonResponse response = new CajonResponse(
                1L,
                "A-001",
                TipoCajon.AUTO,
                EstadoCajon.OCUPADO,
                10L,
                true,
                LocalDateTime.of(2026, 6, 27, 12, 0)
        );

        when(cajonService.updateEstado(
                any(Long.class),
                any(CajonEstadoRequest.class)
        )).thenReturn(response);

        mockMvc.perform(patch("/cajones/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Estado del cajon actualizado correctamente"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.estado").value("OCUPADO"));

        verify(cajonService).updateEstado(
                any(Long.class),
                any(CajonEstadoRequest.class)
        );
    }

    @Test
    void debeRechazarEstadoNulo() throws Exception {
        mockMvc.perform(patch("/cajones/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estado": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.estado").value(
                        "El estado del cajón es obligatorio"
                ));

        verifyNoInteractions(cajonService);
    }

    @Test
    void debeEliminarCajon() throws Exception {
        mockMvc.perform(delete("/cajones/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(cajonService).deleteCajon(1L);
    }

    private CajonRequest crearRequest() {
        return new CajonRequest(
                "A-001",
                TipoCajon.AUTO,
                10L
        );
    }

    private CajonResponse crearResponse() {
        return new CajonResponse(
                1L,
                "A-001",
                TipoCajon.AUTO,
                EstadoCajon.LIBRE,
                10L,
                true,
                LocalDateTime.of(2026, 6, 27, 12, 0)
        );
    }
}

