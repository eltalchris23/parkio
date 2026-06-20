package com.kasaca.parkio.rol.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kasaca.parkio.rol.dto.RolRequest;
import com.kasaca.parkio.rol.dto.RolResponse;
import com.kasaca.parkio.rol.service.RolService;
import com.kasaca.parkio.shared.exception.ConflictException;
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

import java.time.LocalDateTime;
import java.util.List;

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
class RolControllerTest {

    @Mock
    private RolService rolService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RolController rolController = new RolController(rolService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(rolController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @Test
    void debeListarRoles() throws Exception {
        RolResponse response = crearResponse();

        when(rolService.getRoles()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nombre").value("ADMIN"))
                .andExpect(jsonPath("$[0].activo").value(true));

        verify(rolService).getRoles();
    }

    @Test
    void debeObtenerRolPorId() throws Exception {
        RolResponse response = crearResponse();

        when(rolService.getRol(1L)).thenReturn(response);

        mockMvc.perform(get("/api/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("ADMIN"))
                .andExpect(jsonPath("$.activo").value(true));

        verify(rolService).getRol(1L);
    }

    @Test
    void debeResponderNotFoundCuandoRolNoExiste() throws Exception {
        when(rolService.getRol(99L))
                .thenThrow(new ResourceNotFoundException("Rol", 99L));

        mockMvc.perform(get("/api/roles/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Rol con identificador '99' no fue encontrado"))
                .andExpect(jsonPath("$.path").value("/api/roles/99"));
    }

    @Test
    void debeCrearRol() throws Exception {
        RolRequest request = new RolRequest("ADMIN", true);
        RolResponse response = crearResponse();

        when(rolService.addRol(request)).thenReturn(response);

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("ADMIN"))
                .andExpect(jsonPath("$.activo").value(true));

        verify(rolService).addRol(request);
    }

    @Test
    void debeRechazarSolicitudInvalida() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "",
                                  "activo": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("La solicitud contiene datos inválidos"))
                .andExpect(jsonPath("$.validationErrors.nombre")
                        .value("El nombre del rol es obligatorio"))
                .andExpect(jsonPath("$.validationErrors.activo")
                        .value("El estado activo del rol es obligatorio"));

        verifyNoInteractions(rolService);
    }

    @Test
    void debeResponderConflictCuandoNombreEstaDuplicado() throws Exception {
        RolRequest request = new RolRequest("ADMIN", true);

        when(rolService.addRol(request))
                .thenThrow(new ConflictException(
                        "Ya existe un rol con el nombre 'ADMIN'"
                ));

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Ya existe un rol con el nombre 'ADMIN'"));
    }

    @Test
    void debeRechazarJsonMalFormado() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "ADMIN",
                                  "activo":
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("El cuerpo de la solicitud no es válido"));

        verifyNoInteractions(rolService);
    }

    @Test
    void debeActualizarRol() throws Exception {
        RolRequest request = new RolRequest("SUPERVISOR", false);

        RolResponse response = new RolResponse(
                1L,
                "SUPERVISOR",
                false,
                LocalDateTime.of(2026, 6, 20, 12, 0)
        );

        when(rolService.updateRol(1L, request)).thenReturn(response);

        mockMvc.perform(put("/api/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("SUPERVISOR"))
                .andExpect(jsonPath("$.activo").value(false));

        verify(rolService).updateRol(1L, request);
    }

    @Test
    void debeEliminarRol() throws Exception {
        mockMvc.perform(delete("/api/roles/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(rolService).deleteRol(1L);
    }

    private RolResponse crearResponse() {
        return new RolResponse(
                1L,
                "ADMIN",
                true,
                LocalDateTime.of(2026, 6, 20, 12, 0)
        );
    }
}