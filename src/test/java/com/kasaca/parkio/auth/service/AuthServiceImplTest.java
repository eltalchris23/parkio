package com.kasaca.parkio.auth.service;

import com.kasaca.parkio.auth.dto.AuthLoginRequest;
import com.kasaca.parkio.auth.dto.AuthResponse;
import com.kasaca.parkio.security.jwt.JwtService;
import com.kasaca.parkio.shared.exception.UnauthorizedException;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

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
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthServiceImpl authService;

    /**
     * Prepara el servicio de autenticacion con dependencias simuladas.
     */
    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(usuarioRepository, passwordEncoder, jwtService);
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
     * Construye un usuario minimo para validar el flujo de autenticacion.
     */
    private Usuario crearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setEmail("christian@parkio.com");
        usuario.setPasswordHash("hash-bcrypt");
        usuario.setActivo(true);
        return usuario;
    }
}
