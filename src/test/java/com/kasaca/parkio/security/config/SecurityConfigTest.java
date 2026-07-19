package com.kasaca.parkio.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kasaca.parkio.auth.controller.AuthController;
import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.auth.dto.AuthResponse;
import com.kasaca.parkio.auth.service.AuthService;
import com.kasaca.parkio.catalogo.controller.CatalogoController;
import com.kasaca.parkio.catalogo.dto.CatalogoResponse;
import com.kasaca.parkio.catalogo.service.CatalogoService;
import com.kasaca.parkio.cajon.controller.CajonController;
import com.kasaca.parkio.cajon.dto.CajonEstadoRequest;
import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.entity.TipoCajon;
import com.kasaca.parkio.cajon.service.CajonService;
import com.kasaca.parkio.estacionamiento.controller.EstacionamientoController;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoResponse;
import com.kasaca.parkio.estacionamiento.service.EstacionamientoService;
import com.kasaca.parkio.rol.controller.RolController;
import com.kasaca.parkio.rol.dto.RolResponse;
import com.kasaca.parkio.rol.service.RolService;
import com.kasaca.parkio.security.authorization.UsuarioSecurity;
import com.kasaca.parkio.security.cors.CorsConfig;
import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.shared.exception.GlobalExceptionHandler;
import com.kasaca.parkio.usuario.controller.UsuarioController;
import com.kasaca.parkio.usuario.dto.UsuarioCreateRequest;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        AuthController.class,
        UsuarioController.class,
        RolController.class,
        EstacionamientoController.class,
        CajonController.class,
        CatalogoController.class
})
@Import({
        SecurityConfig.class,
        // Importa la configuracion CORS para probarla integrada con Spring Security.
        CorsConfig.class,
        RestAuthenticationEntryPoint.class,
        GlobalExceptionHandler.class,
        UsuarioSecurity.class
})
@TestPropertySource(properties = {
        "parkio.security.jwt.issuer=parkio-test",
        "parkio.security.jwt.secret=clave-de-prueba-con-longitud-suficiente",
        "parkio.security.jwt.expiration-minutes=60",
        // Origenes frontend permitidos durante la prueba.
        "parkio.cors.allowed-origins=http://localhost:4200,http://localhost:5173",
        // Metodos HTTP que el navegador puede usar contra la API.
        "parkio.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        // Headers que el frontend puede enviar al backend.
        "parkio.cors.allowed-headers=Authorization,Content-Type,X-Transaction-Id",
        // Headers que JavaScript del frontend puede leer desde la respuesta.
        "parkio.cors.exposed-headers=X-Transaction-Id",
        // Tiempo que el navegador puede cachear la respuesta preflight.
        "parkio.cors.max-age-seconds=3600"
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
    private EstacionamientoService estacionamientoService;

    @MockitoBean
    private CajonService cajonService;

    @MockitoBean
    private CatalogoService catalogoService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    /**
     * Verifica que CORS permita una peticion preflight desde un origen frontend configurado.
     *
     * <p>Una peticion preflight es una llamada OPTIONS que el navegador envia antes
     * de la peticion real cuando la llamada incluye headers especiales, como
     * Authorization o X-Transaction-Id.</p>
     *
     * <p>Si este preflight falla, el navegador bloquea la llamada real antes de que
     * el frontend pueda consumir el endpoint.</p>
     */
    @Test
    void debePermitirPreflightCorsDesdeOrigenConfigurado() throws Exception {
        mockMvc.perform(options("/roles")
                        // Simula el dominio desde donde corre el frontend.
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")

                        // Indica al backend que el navegador quiere hacer una peticion GET real despues del preflight.
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")

                        // Indica los headers que el frontend quiere enviar en la peticion real.
                        // Authorization es necesario para mandar el JWT.
                        // X-Transaction-Id es necesario para trazabilidad entre frontend y backend.
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Authorization,Content-Type,X-Transaction-Id"
                        ))

                // Si CORS acepta el origen, metodo y headers, Spring responde 200.
                .andExpect(status().isOk())

                // Confirma que el backend permite exactamente el origen del frontend.
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:4200"
                ))

                // Confirma que el metodo GET esta permitido para peticiones CORS.
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        containsString("GET")
                ))

                // Confirma que el frontend puede enviar el header Authorization con el JWT.
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("Authorization")
                ))

                // Confirma que el frontend puede enviar X-Transaction-Id para trazabilidad.
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("X-Transaction-Id")
                ))

                // Confirma que el navegador puede cachear esta validacion preflight durante 3600 segundos.
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_MAX_AGE,
                        "3600"
                ));
    }

    /**
     * Verifica que CORS rechace una peticion preflight desde un origen no configurado.
     *
     * <p>Esto protege al backend para que un sitio web no autorizado no pueda
     * consumir la API desde JavaScript en el navegador.</p>
     *
     * <p>Nota: CORS es una proteccion aplicada por navegadores. No reemplaza JWT ni
     * autorizacion por roles; solo controla que origenes web pueden hacer llamadas
     * desde frontend.</p>
     */
    @Test
    void debeRechazarPreflightCorsDesdeOrigenNoConfigurado() throws Exception {
        mockMvc.perform(options("/roles")
                        // Simula un sitio externo que no esta dentro de parkio.cors.allowed-origins.
                        .header(HttpHeaders.ORIGIN, "https://sitio-no-permitido.com")

                        // Indica que el sitio externo quiere hacer una peticion GET real.
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")

                        // Indica que la peticion real intentaria enviar Authorization.
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))

                // Spring rechaza el preflight porque el origen no esta permitido.
                .andExpect(status().isForbidden());
    }

    /**
     * Verifica que una respuesta real exponga X-Transaction-Id al frontend.
     *
     * <p>El backend siempre puede enviar X-Transaction-Id como header, pero el
     * navegador no permite que JavaScript lea cualquier header automaticamente.</p>
     *
     * <p>Para que el frontend pueda leer X-Transaction-Id, la respuesta debe incluir
     * Access-Control-Expose-Headers con ese header.</p>
     */
    @Test
    void debeExponerTransactionIdEnRespuestaCorsReal() throws Exception {
        RolResponse response = new RolResponse(
                1L,
                "ADMIN",
                true,
                LocalDateTime.of(2026, 7, 7, 9, 0)
        );

        PageResponse<RolResponse> pageResponse = PageResponse.from(
                new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1)
        );

        // Simula la respuesta del service porque esta prueba se enfoca en seguridad/CORS, no en la logica de roles.
        when(rolService.getRoles(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/roles")
                        // Simula una peticion real enviada desde el frontend permitido.
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")

                        // Parametros normales del endpoint paginado.
                        .param("page", "0")
                        .param("size", "10")

                        // Simula un JWT con rol ADMIN para que la seguridad por roles permita consultar /roles.
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))

                // Confirma que la peticion real fue aceptada.
                .andExpect(status().isOk())

                // Confirma que CORS permite responderle al origen del frontend.
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:4200"
                ))

                // Confirma que el navegador podra leer X-Transaction-Id desde JavaScript.
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        containsString("X-Transaction-Id")
                ))

                // Confirma que el body estandarizado tambien contiene el transactionId.
                .andExpect(jsonPath("$.transactionId").isNotEmpty());

        // Confirma que, despues de pasar CORS y seguridad, la peticion llego al service.
        verify(rolService).getRoles(any());
    }

    /**
     * Verifica que el endpoint de login sea publico y no requiera JWT.
     */
    @Test
    void debePermitirLoginSinToken() throws Exception {
        AuthLoginRequest request = new AuthLoginRequest("christian@parkio.com", "clave");
        AuthResponse response = new AuthResponse("jwt-generado", "Bearer", 3600L);

        when(authService.login(request)).thenReturn(response);

        mockMvc.perform(post("/auth/login")
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

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.email").value("christian@parkio.com"));
    }

    /**
     * Comprueba que un endpoint distinto al login y al alta de usuarios requiera autenticacion JWT.
     */
    @Test
    void debeProtegerEndpointsSinToken() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Autenticacion requerida"))
                .andExpect(jsonPath("$.path").value("/usuarios"));
    }

    /**
     * Verifica que un usuario autenticado sin rol ADMIN no pueda acceder al modulo de roles.
     */
    @Test
    void debeRechazarRolesCuandoUsuarioNoTieneRolAdmin() throws Exception {
        mockMvc.perform(get("/roles")
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

        PageResponse<RolResponse> pageResponse = PageResponse.from(
                new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1)
        );

        when(rolService.getRoles(any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/roles")
                        .param("page", "0")
                        .param("size", "10")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].nombre").value("ADMIN"));

        verify(rolService).getRoles(any());
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

        PageResponse<UsuarioResponse> pageResponse = PageResponse.from(
                new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1)
        );

        when(usuarioService.getAllUsers(any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/usuarios")
                        .param("page", "0")
                        .param("size", "10")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].email").value("christian@parkio.com"));

        verify(usuarioService).getAllUsers(any());
    }

    /**
     * Verifica que USER no pueda listar todos los usuarios.
     */
    @Test
    void debeRechazarListarUsuariosCuandoTieneRolUser() throws Exception {
        mockMvc.perform(get("/usuarios")
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

        mockMvc.perform(get("/usuarios/1")
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("usuarioId", 1L))
                                .authorities(() -> "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.email").value("christian@parkio.com"));

        verify(usuarioService).getUserById(1L);
    }

    /**
     * Verifica que USER no pueda consultar el usuario de otra persona.
     */
    @Test
    void debeRechazarConsultarUsuarioAjenoCuandoTieneRolUser() throws Exception {
        mockMvc.perform(get("/usuarios/2")
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

        mockMvc.perform(get("/usuarios/1")
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("usuarioId", 1L))
                                .authorities(() -> "ROLE_OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.roles[0]").value("OPERADOR"));

        verify(usuarioService).getUserById(1L);
    }

    /**
     * Verifica que OPERADOR no pueda consultar el usuario de otra persona.
     */
    @Test
    void debeRechazarConsultarUsuarioAjenoCuandoTieneRolOperador() throws Exception {
        mockMvc.perform(get("/usuarios/2")
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("usuarioId", 1L))
                                .authorities(() -> "ROLE_OPERADOR")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    /**
     * Verifica que ADMIN pueda listar estacionamientos.
     */
    @Test
    void debePermitirListarEstacionamientosCuandoTieneRolAdmin() throws Exception {
        EstacionamientoResponse response = new EstacionamientoResponse(
                1L,
                "Estacionamiento Centro",
                "Sucursal centro",
                new BigDecimal("19.43260000"),
                new BigDecimal("-99.13320000"),
                null,
                true,
                LocalDateTime.of(2026, 7, 7, 9, 0)
        );

        PageResponse<EstacionamientoResponse> pageResponse = PageResponse.from(
                new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1)
        );

        when(estacionamientoService.getEstacionamientos(any(), any(Jwt.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/estacionamientos")
                        .param("page", "0")
                        .param("size", "10")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].nombre").value("Estacionamiento Centro"));

        verify(estacionamientoService).getEstacionamientos(any(), any(Jwt.class));
    }

    /**
     * Verifica que OPERADOR pueda listar estacionamientos.
     */
    @Test
    void debePermitirListarEstacionamientosCuandoTieneRolOperador() throws Exception {
        when(estacionamientoService.getEstacionamientos(any(), any(Jwt.class)))
                .thenReturn(PageResponse.from(
                        new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)
                ));

        mockMvc.perform(get("/estacionamientos")
                        .param("page", "0")
                        .param("size", "10")
                        .with(jwt().authorities(() -> "ROLE_OPERADOR")))
                .andExpect(status().isOk());

        verify(estacionamientoService).getEstacionamientos(any(), any(Jwt.class));
    }

    /**
     * Verifica que USER pueda listar estacionamientos.
     */
    @Test
    void debePermitirListarEstacionamientosCuandoTieneRolUser() throws Exception {
        when(estacionamientoService.getEstacionamientos(any(), any(Jwt.class)))
                .thenReturn(PageResponse.from(
                        new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)
                ));

        mockMvc.perform(get("/estacionamientos")
                        .param("page", "0")
                        .param("size", "10")
                        .with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isOk());

        verify(estacionamientoService).getEstacionamientos(any(), any(Jwt.class));
    }

    /**
     * Verifica que ADMIN pueda consultar un estacionamiento por identificador.
     */
    @Test
    void debePermitirConsultarEstacionamientoCuandoTieneRolAdmin() throws Exception {
        EstacionamientoResponse response = new EstacionamientoResponse(
                1L,
                "Estacionamiento Centro",
                "Sucursal centro",
                new BigDecimal("19.43260000"),
                new BigDecimal("-99.13320000"),
                null,
                true,
                LocalDateTime.of(2026, 7, 7, 9, 0)
        );

        when(estacionamientoService.getEstacionamientoById(any(Long.class), any(Jwt.class))).thenReturn(response);

        mockMvc.perform(get("/estacionamientos/1")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.nombre").value("Estacionamiento Centro"));

        verify(estacionamientoService).getEstacionamientoById(any(Long.class), any(Jwt.class));
    }

    /**
     * Verifica que OPERADOR no pueda crear estacionamientos.
     */
    @Test
    void debeRechazarCrearEstacionamientoCuandoTieneRolOperador() throws Exception {
        EstacionamientoRequest request = new EstacionamientoRequest(
                "Estacionamiento Centro",
                "Sucursal centro",
                new BigDecimal("19.43260000"),
                new BigDecimal("-99.13320000")
        );

        mockMvc.perform(post("/estacionamientos")
                        .with(jwt().authorities(() -> "ROLE_OPERADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(estacionamientoService);
    }

    /**
     * Verifica que ADMIN pueda crear estacionamientos.
     */
    @Test
    void debePermitirCrearEstacionamientoCuandoTieneRolAdmin() throws Exception {
        EstacionamientoRequest request = new EstacionamientoRequest(
                "Estacionamiento Centro",
                "Sucursal centro",
                new BigDecimal("19.43260000"),
                new BigDecimal("-99.13320000")
        );
        EstacionamientoResponse response = new EstacionamientoResponse(
                1L,
                "Estacionamiento Centro",
                "Sucursal centro",
                new BigDecimal("19.43260000"),
                new BigDecimal("-99.13320000"),
                null,
                true,
                LocalDateTime.of(2026, 7, 7, 9, 0)
        );

        when(estacionamientoService.addEstacionamiento(any(EstacionamientoRequest.class), any(Jwt.class)))
                .thenReturn(response);

        mockMvc.perform(post("/estacionamientos")
                        .with(jwt().authorities(() -> "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.nombre").value("Estacionamiento Centro"));

        verify(estacionamientoService).addEstacionamiento(any(EstacionamientoRequest.class), any(Jwt.class));
    }

    /**
     * Verifica que USER no pueda actualizar estacionamientos.
     */
    @Test
    void debeRechazarActualizarEstacionamientoCuandoTieneRolUser() throws Exception {
        EstacionamientoRequest request = new EstacionamientoRequest(
                "Estacionamiento Centro",
                "Sucursal centro",
                new BigDecimal("19.43260000"),
                new BigDecimal("-99.13320000")
        );

        mockMvc.perform(put("/estacionamientos/1")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(estacionamientoService);
    }

    /**
     * Verifica que solo ADMIN pueda eliminar estacionamientos.
     */
    @Test
    void debeRechazarEliminarEstacionamientoCuandoTieneRolOperador() throws Exception {
        mockMvc.perform(delete("/estacionamientos/1")
                        .with(jwt().authorities(() -> "ROLE_OPERADOR")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(estacionamientoService);
    }

    /**
     * Verifica que ADMIN pueda listar cajones.
     */
    @Test
    void debePermitirListarCajonesCuandoTieneRolAdmin() throws Exception {
        CajonResponse response = crearCajonResponse(EstadoCajon.LIBRE);
        PageResponse<CajonResponse> pageResponse = PageResponse.from(
                new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1)
        );

        when(cajonService.getCajones(any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/cajones")
                        .param("page", "0")
                        .param("size", "10")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].numero").value("A-001"));

        verify(cajonService).getCajones(any());
    }

    /**
     * Verifica que OPERADOR pueda listar cajones.
     */
    @Test
    void debePermitirListarCajonesCuandoTieneRolOperador() throws Exception {
        when(cajonService.getCajones(any()))
                .thenReturn(PageResponse.from(
                        new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)
                ));

        mockMvc.perform(get("/cajones")
                        .param("page", "0")
                        .param("size", "10")
                        .with(jwt().authorities(() -> "ROLE_OPERADOR")))
                .andExpect(status().isOk());

        verify(cajonService).getCajones(any());
    }

    /**
     * Verifica que USER pueda listar cajones.
     */
    @Test
    void debePermitirListarCajonesCuandoTieneRolUser() throws Exception {
        when(cajonService.getCajones(any()))
                .thenReturn(PageResponse.from(
                        new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)
                ));

        mockMvc.perform(get("/cajones")
                        .param("page", "0")
                        .param("size", "10")
                        .with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isOk());

        verify(cajonService).getCajones(any());
    }

    /**
     * Verifica que ADMIN pueda listar cajones filtrados por estacionamiento.
     */
    @Test
    void debePermitirListarCajonesPorEstacionamientoCuandoTieneRolAdmin() throws Exception {
        CajonResponse response = crearCajonResponse(EstadoCajon.LIBRE);
        PageResponse<CajonResponse> pageResponse = PageResponse.from(
                new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1)
        );

        when(cajonService.getCajonesByEstacionamientoId(any(Long.class), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/cajones")
                        .param("estacionamientoId", "10")
                        .param("page", "0")
                        .param("size", "10")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].estacionamientoId").value(10L));

        verify(cajonService).getCajonesByEstacionamientoId(any(Long.class), any());
    }

    /**
     * Verifica que USER pueda consultar un cajón por identificador.
     */
    @Test
    void debePermitirConsultarCajonCuandoTieneRolUser() throws Exception {
        CajonResponse response = crearCajonResponse(EstadoCajon.LIBRE);

        when(cajonService.getCajon(1L)).thenReturn(response);

        mockMvc.perform(get("/cajones/1")
                        .with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.numero").value("A-001"));

        verify(cajonService).getCajon(1L);
    }

    /**
     * Verifica que solo ADMIN pueda crear cajones.
     */
    @Test
    void debeRechazarCrearCajonCuandoTieneRolOperador() throws Exception {
        CajonRequest request = crearCajonRequest();

        mockMvc.perform(post("/cajones")
                        .with(jwt().authorities(() -> "ROLE_OPERADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cajonService);
    }

    /**
     * Verifica que ADMIN pueda crear cajones.
     */
    @Test
    void debePermitirCrearCajonCuandoTieneRolAdmin() throws Exception {
        CajonRequest request = crearCajonRequest();
        CajonResponse response = crearCajonResponse(EstadoCajon.LIBRE);

        when(cajonService.addCajon(request)).thenReturn(response);

        mockMvc.perform(post("/cajones")
                        .with(jwt().authorities(() -> "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.numero").value("A-001"));

        verify(cajonService).addCajon(request);
    }

    /**
     * Verifica que solo ADMIN pueda actualizar cajones.
     */
    @Test
    void debeRechazarActualizarCajonCuandoTieneRolUser() throws Exception {
        CajonRequest request = crearCajonRequest();

        mockMvc.perform(put("/cajones/1")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cajonService);
    }

    /**
     * Verifica que OPERADOR pueda cambiar el estado de un cajón.
     */
    @Test
    void debePermitirCambiarEstadoCajonCuandoTieneRolOperador() throws Exception {
        CajonEstadoRequest request = new CajonEstadoRequest(EstadoCajon.OCUPADO);
        CajonResponse response = crearCajonResponse(EstadoCajon.OCUPADO);

        when(cajonService.updateEstado(1L, request)).thenReturn(response);

        mockMvc.perform(patch("/cajones/1/estado")
                        .with(jwt().authorities(() -> "ROLE_OPERADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.estado").value("OCUPADO"));

        verify(cajonService).updateEstado(1L, request);
    }

    /**
     * Verifica que USER no pueda cambiar el estado de un cajón.
     */
    @Test
    void debeRechazarCambiarEstadoCajonCuandoTieneRolUser() throws Exception {
        CajonEstadoRequest request = new CajonEstadoRequest(EstadoCajon.OCUPADO);

        mockMvc.perform(patch("/cajones/1/estado")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cajonService);
    }

    /**
     * Verifica que ADMIN pueda eliminar cajones.
     */
    @Test
    void debePermitirEliminarCajonCuandoTieneRolAdmin() throws Exception {
        mockMvc.perform(delete("/cajones/1")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isNoContent());

        verify(cajonService).deleteCajon(1L);
    }

    /**
     * Verifica que OPERADOR no pueda eliminar cajones.
     */
    @Test
    void debeRechazarEliminarCajonCuandoTieneRolOperador() throws Exception {
        mockMvc.perform(delete("/cajones/1")
                        .with(jwt().authorities(() -> "ROLE_OPERADOR")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cajonService);
    }

    /**
     * Verifica que los catalogos de tipos de cajon requieran autenticacion JWT.
     */
    @Test
    void debeRechazarCatalogoTiposCajonSinToken() throws Exception {
        mockMvc.perform(get("/catalogos/cajones/tipos"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(catalogoService);
    }

    /**
     * Verifica que USER pueda consultar catalogos de tipos de cajon.
     */
    @Test
    void debePermitirCatalogoTiposCajonCuandoTieneRolUser() throws Exception {
        when(catalogoService.getTiposCajon()).thenReturn(List.of(
                new CatalogoResponse("AUTO", "Auto")
        ));

        mockMvc.perform(get("/catalogos/cajones/tipos")
                        .with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].codigo").value("AUTO"));

        verify(catalogoService).getTiposCajon();
    }

    /**
     * Verifica que OPERADOR pueda consultar catalogos de estados de cajon.
     */
    @Test
    void debePermitirCatalogoEstadosCajonCuandoTieneRolOperador() throws Exception {
        when(catalogoService.getEstadosCajon()).thenReturn(List.of(
                new CatalogoResponse("LIBRE", "Libre")
        ));

        mockMvc.perform(get("/catalogos/cajones/estados")
                        .with(jwt().authorities(() -> "ROLE_OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].codigo").value("LIBRE"));

        verify(catalogoService).getEstadosCajon();
    }

    /**
     * Crea una solicitud reutilizable para las pruebas de seguridad del módulo Cajón.
     *
     * @return solicitud válida para crear o actualizar un cajón
     */
    private CajonRequest crearCajonRequest() {
        return new CajonRequest("A-001", TipoCajon.AUTO, 10L);
    }

    /**
     * Crea una respuesta reutilizable para las pruebas de seguridad del módulo Cajón.
     *
     * @param estado estado que tendrá el cajón en la respuesta
     * @return respuesta válida de un cajón
     */
    private CajonResponse crearCajonResponse(EstadoCajon estado) {
        return new CajonResponse(
                1L,
                "A-001",
                TipoCajon.AUTO,
                estado,
                10L,
                true,
                LocalDateTime.of(2026, 7, 7, 9, 0)
        );
    }
}

