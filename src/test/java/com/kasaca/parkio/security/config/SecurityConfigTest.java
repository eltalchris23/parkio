package com.kasaca.parkio.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasaca.parkio.auth.controller.AuthController;
import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.auth.dto.AuthResponse;
import com.kasaca.parkio.auth.service.AuthService;
import com.kasaca.parkio.shared.exception.GlobalExceptionHandler;
import com.kasaca.parkio.usuario.controller.UsuarioController;
import com.kasaca.parkio.usuario.dto.UsuarioCreateRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, UsuarioController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "parkio.security.jwt.issuer=parkio-test",
        "parkio.security.jwt.secret=clave-de-prueba-con-longitud-suficiente",
        "parkio.security.jwt.expiration-minutes=60"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    /**
     * Verifica que el endpoint de login sea publico y no requiera JWT.
     */
    @Test
    void debePermitirLoginSinToken() throws Exception {
        AuthLoginRequest request = new AuthLoginRequest("christian@parkio.com", "clave");
        AuthResponse response = new AuthResponse("jwt-generado", "Bearer", 3600L);

        when(authService.login(request)).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-generado"));
    }

    /**
     * Verifica que la creacion de usuarios sea publica para permitir el
     * registro inicial antes de contar con un token JWT.
     */
    @Test
    void debePermitirCrearUsuarioSinToken() throws Exception {
        UsuarioCreateRequest request = new UsuarioCreateRequest(
                "Christian",
                "Salazar",
                "christian@parkio.com",
                "clave"
        );
        UsuarioResponse response = new UsuarioResponse(
                1L,
                "Christian",
                "Salazar",
                "christian@parkio.com",
                true,
                LocalDateTime.of(2026, 7, 7, 9, 0),
                Set.of(),
                Set.of()
        );

        when(usuarioService.addUser(request)).thenReturn(response);

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("christian@parkio.com"));
    }

    /**
     * Comprueba que un endpoint distinto al login y al alta de usuarios requiera autenticacion JWT.
     */
    @Test
    void debeProtegerEndpointsSinToken() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Autenticacion requerida"))
                .andExpect(jsonPath("$.path").value("/api/usuarios"));
    }
}
