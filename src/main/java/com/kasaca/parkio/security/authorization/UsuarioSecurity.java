package com.kasaca.parkio.security.authorization;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Helper de autorizacion para validar reglas relacionadas con usuarios.
 *
 * <p>Centraliza la comparacion entre el usuario solicitado en la URL y el
 * usuario autenticado en el JWT para evitar expresiones SpEL largas o fragiles
 * dentro de los controladores.</p>
 */
@Component("usuarioSecurity")
public class UsuarioSecurity {

    /**
     * Verifica si el usuario autenticado es el mismo usuario solicitado.
     *
     * <p>Lee el claim {@code usuarioId} del JWT y lo compara contra el
     * identificador recibido en la ruta. La comparacion se hace como {@link Long}
     * para evitar problemas cuando el claim sea deserializado como Integer,
     * Long u otro tipo numerico.</p>
     *
     * @param authentication autenticacion actual creada por Spring Security
     * @param usuarioId identificador del usuario solicitado en el endpoint
     * @return {@code true} si el JWT pertenece al mismo usuario solicitado
     */
    public boolean isSelf(Authentication authentication, Long usuarioId) {
        if (authentication == null || usuarioId == null) {
            return false;
        }

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return false;
        }

        Jwt jwt = jwtAuthenticationToken.getToken();
        Object claimUsuarioId = jwt.getClaim("usuarioId");

        if (claimUsuarioId instanceof Number number) {
            return usuarioId.equals(number.longValue());
        }

        if (claimUsuarioId instanceof String text) {
            try {
                return usuarioId.equals(Long.valueOf(text));
            } catch (NumberFormatException exception) {
                return false;
            }
        }

        return false;
    }
}
