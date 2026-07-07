package com.kasaca.parkio.security.jwt;

import com.kasaca.parkio.rol.entity.Rol;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    private JwtService jwtService;

    /**
     * Configura el servicio JWT con propiedades controladas para pruebas.
     */
    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "parkio-test",
                "clave-de-prueba-con-longitud-suficiente",
                60
        );
        jwtService = new JwtService(jwtEncoder, properties);
    }

    /**
     * Verifica que el token devuelto provenga del codificador configurado.
     */
    @Test
    void debeGenerarTokenConValorCodificado() {
        Usuario usuario = crearUsuario();
        Jwt jwt = new Jwt(
                "token-firmado",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "christian@parkio.com")
        );

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        String token = jwtService.generateToken(usuario);

        assertEquals("token-firmado", token);
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    /**
     * Comprueba que los claims del JWT incluyan identidad y roles sin exponer passwordHash.
     */
    @Test
    void debeConstruirClaimsDelUsuarioAutenticado() {
        Usuario usuario = crearUsuario();
        Jwt jwt = new Jwt(
                "token-firmado",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "christian@parkio.com")
        );
        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        jwtService.generateToken(usuario);

        verify(jwtEncoder).encode(captor.capture());
        JwtClaimsSet claims = captor.getValue().getClaims();
        JwsHeader headers = captor.getValue().getJwsHeader();

        assertEquals(MacAlgorithm.HS256, headers.getAlgorithm());
        assertEquals("parkio-test", claims.getClaimAsString("iss"));
        assertEquals("christian@parkio.com", claims.getSubject());
        assertEquals(1L, ((Number) claims.getClaim("usuarioId")).longValue());
        assertEquals(List.of("ADMIN"), claims.getClaim("roles"));
        assertFalse(claims.getClaims().containsKey("passwordHash"));
    }

    /**
     * Verifica con un codificador real que se pueda firmar el JWT usando HS256
     * incluso cuando el usuario autenticado no tiene roles asignados.
     */
    @Test
    void debeGenerarTokenRealConRolesVacios() {
        JwtEncoder realEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                "clave-de-prueba-con-longitud-suficiente".getBytes(StandardCharsets.UTF_8)
        ));
        JwtProperties properties = new JwtProperties(
                "parkio-test",
                "clave-de-prueba-con-longitud-suficiente",
                60
        );
        JwtService realJwtService = new JwtService(realEncoder, properties);
        Usuario usuario = crearUsuarioSinRoles();

        String token = realJwtService.generateToken(usuario);

        assertFalse(token.isBlank());
        assertTrue(token.split("\\.").length == 3);
    }

    /**
     * Verifica la conversion de minutos configurados a segundos de expiracion.
     */
    @Test
    void debeRegresarExpiracionEnSegundos() {
        assertEquals(3600L, jwtService.getExpirationSeconds());
    }

    /**
     * Construye un usuario con rol para validar los claims del token.
     */
    private Usuario crearUsuario() {
        Rol rol = new Rol();
        rol.setId(2L);
        rol.setNombre("ADMIN");

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("christian@parkio.com");
        usuario.setPasswordHash("hash-no-expuesto");
        usuario.setRoles(Set.of(rol));

        return usuario;
    }

    /**
     * Construye un usuario sin roles para validar que una lista vacia no impida
     * la generacion del token.
     */
    private Usuario crearUsuarioSinRoles() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("christian@parkio.com");
        usuario.setPasswordHash("hash-no-expuesto");
        usuario.setRoles(new HashSet<>());

        return usuario;
    }
}
