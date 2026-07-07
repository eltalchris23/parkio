package com.kasaca.parkio.security.jwt;

import com.kasaca.parkio.usuario.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Servicio encargado de generar tokens JWT para usuarios autenticados.
 *
 * <p>Centraliza la construccion de claims para evitar que controladores o
 * servicios de negocio manipulen directamente la estructura del token.</p>
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    /**
     * Genera un JWT firmado para el usuario autenticado.
     *
     * <p>El token incluye identificador del usuario, correo y roles como claims.
     * No incluye contrasena ni passwordHash.</p>
     */
    public String generateToken(Usuario usuario) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(getExpirationSeconds());

        List<String> roles = usuario.getRoles()
                .stream()
                .map(rol -> rol.getNombre())
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(usuario.getEmail())
                .claim("usuarioId", usuario.getId())
                .claim("roles", roles)
                .build();

        JwsHeader jwsHeader = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims))
                .getTokenValue();
    }

    /**
     * Devuelve la duracion configurada del token expresada en segundos.
     */
    public long getExpirationSeconds() {
        return jwtProperties.expirationMinutes() * 60;
    }
}
