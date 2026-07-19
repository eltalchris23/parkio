package com.kasaca.parkio.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.auth.dto.AuthResponse;
import com.kasaca.parkio.auth.service.AuthService;
import com.kasaca.parkio.shared.exception.GlobalExceptionHandler;
import com.kasaca.parkio.shared.exception.UnauthorizedException;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
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

import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    /**
     * Configura MockMvc con el controlador de autenticacion y el manejador global.
     */
    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    /**
     * Verifica que un login exitoso devuelva token, tipo y expiracion.
     */
    @Test
    void debeIniciarSesion() throws Exception {
        AuthLoginRequest request = new AuthLoginRequest("christian@parkio.com", "clave");
        AuthResponse response = new AuthResponse("jwt-generado", "Bearer", 3600L);

        when(authService.login(request)).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-generado"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600L));

        verify(authService).login(request);
    }

    /**
     * Comprueba que credenciales invalidas respondan con HTTP 401.
     */
    @Test
    void debeResponderUnauthorizedCuandoCredencialesSonInvalidas() throws Exception {
        AuthLoginRequest request = new AuthLoginRequest("christian@parkio.com", "incorrecta");
        when(authService.login(request)).thenThrow(new UnauthorizedException("Credenciales invalidas"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciales invalidas"));
    }

    /**
     * Verifica que Jakarta Validation rechace una solicitud de login incompleta.
     */
    @Test
    void debeRechazarLoginInvalido() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "correo-invalido",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());

        verifyNoInteractions(authService);
    }

    /**
     * Verifica que el endpoint /auth/me devuelva la informacion vigente del usuario autenticado.
     */
    @Test
    void debeConsultarUsuarioAutenticado() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token-prueba")
                .header("alg", "HS256")
                .claim("usuarioId", 1L)
                .build();
        UsuarioResponse response = new UsuarioResponse(
                1L,
                "Christian",
                "Hernandez",
                "christian@parkio.com",
                true,
                LocalDateTime.of(2026, 7, 18, 10, 0),
                Set.of("ADMIN"),
                Set.of(1L)
        );

        when(authService.getCurrentUser(jwt)).thenReturn(response);

        try {
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

            mockMvc.perform(get("/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Usuario autenticado consultado correctamente"))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.nombre").value("Christian"))
                    .andExpect(jsonPath("$.data.email").value("christian@parkio.com"))
                    .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"))
                    .andExpect(jsonPath("$.data.estacionamientoIds[0]").value(1L));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(authService).getCurrentUser(jwt);
    }
}

