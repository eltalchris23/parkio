package com.kasaca.parkio.auth.service;

import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.auth.dto.AuthResponse;
import com.kasaca.parkio.security.jwt.JwtService;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.shared.exception.UnauthorizedException;
import com.kasaca.parkio.usuario.dto.UsuarioResponse;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.mapper.UsuarioMapper;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthServiceImpl authService;

    /**
     * Prepara el servicio de autenticacion con dependencias simuladas.
     */
    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(usuarioRepository, usuarioMapper, passwordEncoder, jwtService);
    }

    /**
     * Verifica que el login genere una respuesta JWT cuando las credenciales son validas.
     */
    @Test
    void debeAutenticarUsuarioConCredencialesValidas() {
        AuthLoginRequest request = new AuthLoginRequest("christian@parkio.com", "clave");
        Usuario usuario = crearUsuario();

        when(usuarioRepository.findByEmailAndActivoTrue("christian@parkio.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave", "hash-bcrypt")).thenReturn(true);
        when(jwtService.generateToken(usuario)).thenReturn("jwt-generado");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(request);

        assertEquals("jwt-generado", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());

        verify(usuarioRepository).findByEmailAndActivoTrue("christian@parkio.com");
        verify(passwordEncoder).matches("clave", "hash-bcrypt");
        verify(jwtService).generateToken(usuario);
    }

    /**
     * Comprueba que un correo inexistente se traduzca en credenciales invalidas.
     */
    @Test
    void debeRechazarCorreoInexistente() {
        AuthLoginRequest request = new AuthLoginRequest("nadie@parkio.com", "clave");
        when(usuarioRepository.findByEmailAndActivoTrue("nadie@parkio.com")).thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(request)
        );

        assertEquals("Credenciales invalidas", exception.getMessage());
        verify(usuarioRepository).findByEmailAndActivoTrue("nadie@parkio.com");
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    /**
     * Comprueba que una contrasena incorrecta no genere token JWT.
     */
    @Test
    void debeRechazarPasswordIncorrecto() {
        AuthLoginRequest request = new AuthLoginRequest("christian@parkio.com", "incorrecta");
        Usuario usuario = crearUsuario();

        when(usuarioRepository.findByEmailAndActivoTrue("christian@parkio.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "hash-bcrypt")).thenReturn(false);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(request)
        );

        assertEquals("Credenciales invalidas", exception.getMessage());
        verify(usuarioRepository).findByEmailAndActivoTrue("christian@parkio.com");
        verify(passwordEncoder).matches("incorrecta", "hash-bcrypt");
        verifyNoInteractions(jwtService);
    }

    /**
     * Verifica que el servicio consulte en base de datos el usuario actual indicado por el JWT.
     */
    @Test
    void debeConsultarUsuarioActualDesdeJwt() {
        Jwt jwt = crearJwtConUsuarioId(1L);
        Usuario usuario = crearUsuario();
        UsuarioResponse usuarioResponse = crearUsuarioResponse();

        when(usuarioRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toResponse(usuario)).thenReturn(usuarioResponse);

        UsuarioResponse response = authService.getCurrentUser(jwt);

        assertEquals(1L, response.id());
        assertEquals("christian@parkio.com", response.email());
        assertEquals(Set.of("ADMIN"), response.roles());

        verify(usuarioRepository).findByIdAndActivoTrue(1L);
        verify(usuarioMapper).toResponse(usuario);
    }

    /**
     * Comprueba que un JWT sin claim usuarioId se trate como token invalido.
     */
    @Test
    void debeRechazarJwtSinUsuarioId() {
        Jwt jwt = crearJwtSinUsuarioId();

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.getCurrentUser(jwt)
        );

        assertEquals("Token invalido", exception.getMessage());
        verifyNoInteractions(usuarioMapper);
    }

    /**
     * Comprueba que un usuario inexistente o inactivo se traduzca en recurso no encontrado.
     */
    @Test
    void debeRechazarUsuarioActualInexistenteOInactivo() {
        Jwt jwt = crearJwtConUsuarioId(99L);
        when(usuarioRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> authService.getCurrentUser(jwt)
        );

        assertEquals("Usuario con identificador '99' no fue encontrado", exception.getMessage());
        verify(usuarioRepository).findByIdAndActivoTrue(99L);
        verifyNoInteractions(usuarioMapper);
    }

    /**
     * Construye un usuario minimo para validar el flujo de autenticacion.
     */
    private Usuario crearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setEmail("christian@parkio.com");
        usuario.setPasswordHash("hash-bcrypt");
        usuario.setActivo(true);
        return usuario;
    }

    /**
     * Construye una respuesta publica de usuario para validar la conversion del mapper.
     */
    private UsuarioResponse crearUsuarioResponse() {
        return new UsuarioResponse(
                1L,
                "Christian",
                "Hernandez",
                "christian@parkio.com",
                true,
                LocalDateTime.of(2026, 7, 18, 10, 0),
                Set.of("ADMIN"),
                Set.of(1L)
        );
    }

    /**
     * Construye un JWT de prueba con el claim usuarioId requerido por /auth/me.
     */
    private Jwt crearJwtConUsuarioId(Long usuarioId) {
        return Jwt.withTokenValue("token-prueba")
                .header("alg", "HS256")
                .subject("christian@parkio.com")
                .issuedAt(Instant.parse("2026-07-18T10:00:00Z"))
                .expiresAt(Instant.parse("2026-07-18T11:00:00Z"))
                .claim("usuarioId", usuarioId)
                .build();
    }

    /**
     * Construye un JWT de prueba sin usuarioId para validar el caso de token incompleto.
     */
    private Jwt crearJwtSinUsuarioId() {
        return Jwt.withTokenValue("token-prueba")
                .header("alg", "HS256")
                .subject("christian@parkio.com")
                .issuedAt(Instant.parse("2026-07-18T10:00:00Z"))
                .expiresAt(Instant.parse("2026-07-18T11:00:00Z"))
                .build();
    }
}
