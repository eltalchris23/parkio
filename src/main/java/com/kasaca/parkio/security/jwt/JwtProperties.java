package com.kasaca.parkio.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades configurables para generacion y validacion de JWT.
 *
 * <p>Los valores se leen desde application.yaml bajo el prefijo
 * parkio.security.jwt.</p>
 */
@ConfigurationProperties(prefix = "parkio.security.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        long expirationMinutes
) {
}
