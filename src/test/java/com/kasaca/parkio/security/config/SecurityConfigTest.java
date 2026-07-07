package com.kasaca.parkio.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasaca.parkio.auth.controller.AuthController;
import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.auth.dto.AuthResponse;
import com.kasaca.parkio.auth.service.AuthService;
import com.kasaca.parkio.rol.controller.RolController;
import com.kasaca.parkio.rol.dto.RolResponse;
import com.kasaca.parkio.rol.service.RolService;
import com.kasaca.parkio.security.authorization.UsuarioSecurity;
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
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, UsuarioController.class, RolController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, GlobalExceptionHandler.class, UsuarioSecurity.class})
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
    private RolService rolService;

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

    /**
     * Verifica que un usuario autenticado sin rol ADMIN no pueda acceder al modulo de roles.
     */
    @Test
    void debeRechazarRolesCuandoUsuarioNoTieneRolAdmin() throws Exception {
        mockMvc.perform(get("/api/roles")
                        .with(jwt().authorities(() -> "ROLE_USUARIO")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(rolService);
    }

    /**
     * Verifica que un usuario autenticado con rol ADMIN pueda acceder al modulo de roles.
     */
    @Test
    void debePermitirRolesCuandoUsuarioTieneRolAdmin() throws Exception {
        RolResponse response = new RolResponse(
                1L,
                "ADMIN",
                true,
                LocalDateTime.of(2026, 7, 7, 9, 0)
        );

        when(rolService.getRoles()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/roles")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nombre").value("ADMIN"));

        verify(rolService).getRoles();
    }

    /**
     * Verifica que listar usuarios sea una operacion exclusiva de ADMIN.
     */
    @Test
    void debePermitirListarUsuariosCuandoTieneRolAdmin() throws Exception {
        UsuarioResponse response = new UsuarioResponse(
                1L,
                "Christian",
                "Salazar",
                "christian@parkio.com",
                true,
                LocalDateTime.of(2026, 7, 7, 9, 0),
                Set.of("ADMIN"),
                Set.of()
        );

        when(usuarioService.getAllUsers()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/usuarios")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].email").value("christian@parkio.com"));

        verify(usuarioService).getAllUsers();
    }

    /**
     * Verifica que USER no pueda listar todos los usuarios.
     */
    @Test
    void debeRechazarListarUsuariosCuandoTieneRolUser() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("usuarioId", 1L))
                                .authorities(() -> "ROLE_USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    /**
     * Verifica que USER pueda consultar su propio usuario.
     */
    @Test
    void debePermitirConsultarUsuarioPropioCuandoTieneRolUser() throws Exception {
        UsuarioResponse response = new UsuarioResponse(
                1L,
                "Christian",
                "Salazar",
                "christian@parkio.com",
                true,
                LocalDateTime.of(2026, 7, 7, 9, 0),
                Set.of("USER"),
                Set.of()
        );

        when(usuarioService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/usuarios/1")
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("usuarioId", 1L))
                                .authorities(() -> "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("christian@parkio.com"));

        verify(usuarioService).getUserById(1L);
    }

    /**
     * Verifica que USER no pueda consultar el usuario de otra persona.
     */
    @Test
    void debeRechazarConsultarUsuarioAjenoCuandoTieneRolUser() throws Exception {
        mockMvc.perform(get("/api/usuarios/2")
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("usuarioId", 1L))
                                .authorities(() -> "ROLE_USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    /**
     * Verifica que OPERADOR pueda consultar su propio usuario.
     */
    @Test
    void debePermitirConsultarUsuarioPropioCuandoTieneRolOperador() throws Exception {
        UsuarioResponse response = new UsuarioResponse(
                1L,
                "Christian",
                "Salazar",
                "christian@parkio.com",
                true,
                LocalDateTime.of(2026, 7, 7, 9, 0),
                Set.of("OPERADOR"),
                Set.of()
        );

        when(usuarioService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/usuarios/1")
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("usuarioId", 1L))
                                .authorities(() -> "ROLE_OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.roles[0]").value("OPERADOR"));

        verify(usuarioService).getUserById(1L);
    }

    /**
     * Verifica que OPERADOR no pueda consultar el usuario de otra persona.
     */
    @Test
    void debeRechazarConsultarUsuarioAjenoCuandoTieneRolOperador() throws Exception {
        mockMvc.perform(get("/api/usuarios/2")
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("usuarioId", 1L))
                                .authorities(() -> "ROLE_OPERADOR")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }
}
