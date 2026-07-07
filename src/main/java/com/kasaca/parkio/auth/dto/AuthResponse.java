package com.kasaca.parkio.auth.dto;

/**
 * Respuesta generada cuando el inicio de sesion es exitoso.
 *
 * <p>Incluye el token JWT que el cliente debera enviar posteriormente en el
 * encabezado Authorization usando el formato Bearer.</p>
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
