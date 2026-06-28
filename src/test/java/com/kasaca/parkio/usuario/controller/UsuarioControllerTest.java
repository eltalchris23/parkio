package com.kasaca.parkio.usuario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.GlobalExceptionHandler;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.usuario.dto.UsuarioRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.service.UsuarioService;
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
import java.util.Set;

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
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    /**
     * Configura MockMvc con el controlador y el manejador global de excepciones.
     */
    @BeforeEach
    void setUp() {
        UsuarioController usuarioController = new UsuarioController(usuarioService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(usuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    /**
     * Verifica que el endpoint de colección devuelva todos los usuarios.
     */
    @Test
    void debeListarUsuarios() throws Exception {
        UsuarioResponse response = crearResponse();
        when(usuarioService.getAllUsers()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].email").value("christian@parkio.com"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());

        verify(usuarioService).getAllUsers();
    }

    /**
     * Comprueba que se pueda consultar un usuario existente por identificador.
     */
    @Test
    void debeObtenerUsuarioPorId() throws Exception {
        UsuarioResponse response = crearResponse();
        when(usuarioService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("Christian"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));

        verify(usuarioService).getUserById(1L);
    }

    /**
     * Verifica la respuesta 404 cuando el usuario solicitado no existe.
     */
    @Test
    void debeResponderNotFoundCuandoUsuarioNoExiste() throws Exception {
        when(usuarioService.getUserById(99L)).thenThrow(new ResourceNotFoundException("Usuario", 99L));

        mockMvc.perform(get("/api/usuarios/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Usuario con identificador '99' no fue encontrado"))
                .andExpect(jsonPath("$.path").value("/api/usuarios/99"));
    }

    /**
     * Comprueba que una solicitud válida cree un usuario con estado HTTP 201.
     */
    @Test
    void debeCrearUsuario() throws Exception {
        UsuarioRequest request = crearRequest();
        UsuarioResponse response = crearResponse();
        when(usuarioService.addUser(request)).thenReturn(response);

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("christian@parkio.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(usuarioService).addUser(request);
    }

    /**
     * Verifica que Jakarta Validation rechace campos obligatorios vacíos.
     */
    @Test
    void debeRechazarSolicitudInvalida() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "",
                                  "apellido": "Salazar",
                                  "email": "correo-invalido",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.nombre").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());

        verifyNoInteractions(usuarioService);
    }

    /**
     * Comprueba que un correo duplicado se traduzca a estado HTTP 409.
     */
    @Test
    void debeResponderConflictCuandoCorreoEstaDuplicado() throws Exception {
        UsuarioRequest request = crearRequest();
        when(usuarioService.addUser(request)).thenThrow(
                new ConflictException("Ya existe un usuario con el correo 'christian@parkio.com'")
        );

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Ya existe un usuario con el correo 'christian@parkio.com'"));
    }

    /**
     * Verifica que un cuerpo JSON mal formado produzca estado HTTP 400.
     */
    @Test
    void debeRechazarJsonMalFormado() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Christian",
                                  "email":
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(usuarioService);
    }

    /**
     * Comprueba que una solicitud válida actualice y devuelva el usuario.
     */
    @Test
    void debeActualizarUsuario() throws Exception {
        UsuarioRequest request = crearRequest();
        UsuarioResponse response = crearResponse();
        when(usuarioService.updateUser(1L, request)).thenReturn(response);

        mockMvc.perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("christian@parkio.com"));

        verify(usuarioService).updateUser(1L, request);
    }

    /**
     * Verifica que eliminar un usuario responda sin contenido.
     */
    @Test
    void debeEliminarUsuario() throws Exception {
        mockMvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(usuarioService).deleteUser(1L);
    }

    /**
     * Construye una solicitud válida reutilizable por las pruebas del controlador.
     */
    private UsuarioRequest crearRequest() {
        return new UsuarioRequest("Christian", "Salazar", "christian@parkio.com", "clave-segura");
    }

    /**
     * Construye una respuesta pública reutilizable por las pruebas del controlador.
     */
    private UsuarioResponse crearResponse() {
        return new UsuarioResponse(1L, "Christian", "Salazar", "christian@parkio.com", true,
                LocalDateTime.of(2026, 6, 28, 12, 0), Set.of("ADMIN"));
    }
}
