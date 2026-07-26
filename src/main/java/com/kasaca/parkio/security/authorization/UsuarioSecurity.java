package com.kasaca.parkio.security.authorization;

import com.kasaca.parkio.estacionamiento.repository.EstacionamientoRepository;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Helper de autorizacion para validar reglas relacionadas con usuarios.
 *
 * <p>Centraliza la comparacion entre el usuario solicitado en la URL y el
 * usuario autenticado en el JWT para evitar expresiones SpEL largas o fragiles
 * dentro de los controladores.</p>
 */
@Component("usuarioSecurity")
@RequiredArgsConstructor
public class UsuarioSecurity {

    private static final String ROLE_OWNER = "ROLE_OWNER";
    private static final String ROL_OPERADOR = "OPERADOR";

    private final UsuarioRepository usuarioRepository;
    private final EstacionamientoRepository estacionamientoRepository;

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
        return getAuthenticatedUsuarioId(authentication)
                .map(authenticatedUsuarioId -> authenticatedUsuarioId.equals(usuarioId))
                .orElse(false);
    }

    /**
     * Verifica si el usuario autenticado con rol OWNER puede administrar al operador indicado.
     *
     * <p>La relacion se considera valida solo cuando el usuario objetivo esta activo,
     * tiene rol OPERADOR activo y esta asignado a por lo menos un estacionamiento activo
     * cuyo owner sea el usuario autenticado. Esto evita que OWNER tenga alcance global
     * sobre todos los usuarios.</p>
     *
     * @param authentication autenticacion actual creada a partir del JWT
     * @param operadorId identificador del usuario operador que se intenta administrar
     * @return {@code true} cuando el operador pertenece al alcance del OWNER autenticado
     */
    public boolean canManageOperador(Authentication authentication, Long operadorId) {
        if (!hasAuthority(authentication, ROLE_OWNER) || operadorId == null) {
            return false;
        }

        return getAuthenticatedUsuarioId(authentication)
                .map(ownerId -> usuarioRepository.existsOperadorActivoAsignadoAEstacionamientoDeOwner(
                        operadorId,
                        ownerId
                ))
                .orElse(false);
    }

    /**
     * Verifica si OWNER puede asignar uno de sus estacionamientos a un operador existente.
     *
     * <p>No asigna roles ni convierte usuarios en OPERADOR. El usuario objetivo ya debe
     * tener rol OPERADOR y el estacionamiento solicitado debe pertenecer al OWNER
     * autenticado.</p>
     *
     * @param authentication autenticacion actual creada a partir del JWT
     * @param operadorId identificador del usuario operador que recibira el estacionamiento
     * @param estacionamientoId identificador del estacionamiento que se desea asignar
     * @return {@code true} cuando OWNER, operador y estacionamiento cumplen el alcance permitido
     */
    public boolean canAssignOwnEstacionamientoToOperador(
            Authentication authentication,
            Long operadorId,
            Long estacionamientoId
    ) {
        if (!hasAuthority(authentication, ROLE_OWNER) || operadorId == null || estacionamientoId == null) {
            return false;
        }

        return getAuthenticatedUsuarioId(authentication)
                .map(ownerId -> usuarioRepository.existsUsuarioActivoConRol(operadorId, ROL_OPERADOR)
                        && estacionamientoRepository.existsByIdAndOwnerIdAndActivoTrue(estacionamientoId, ownerId))
                .orElse(false);
    }

    /**
     * Verifica si OWNER puede retirar uno de sus estacionamientos de un operador.
     *
     * <p>La relacion debe existir previamente, el usuario objetivo debe ser OPERADOR
     * activo y el estacionamiento debe pertenecer al OWNER autenticado.</p>
     *
     * @param authentication autenticacion actual creada a partir del JWT
     * @param operadorId identificador del usuario operador
     * @param estacionamientoId identificador del estacionamiento que se desea retirar
     * @return {@code true} cuando la relacion esta dentro del alcance del OWNER autenticado
     */
    public boolean canRemoveOwnEstacionamientoFromOperador(
            Authentication authentication,
            Long operadorId,
            Long estacionamientoId
    ) {
        if (!hasAuthority(authentication, ROLE_OWNER) || operadorId == null || estacionamientoId == null) {
            return false;
        }

        return getAuthenticatedUsuarioId(authentication)
                .map(ownerId -> usuarioRepository.existsOperadorActivoAsignadoAEstacionamientoPropio(
                        operadorId,
                        ownerId,
                        estacionamientoId
                ))
                .orElse(false);
    }

    /**
     * Extrae el identificador del usuario autenticado desde el claim {@code usuarioId}.
     *
     * <p>El claim puede llegar como numero o texto dependiendo de la serializacion
     * del JWT. Por eso se normaliza a {@link Long} antes de compararlo.</p>
     */
    private Optional<Long> getAuthenticatedUsuarioId(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return Optional.empty();
        }

        Jwt jwt = jwtAuthenticationToken.getToken();
        Object claimUsuarioId = jwt.getClaim("usuarioId");

        if (claimUsuarioId instanceof Number number) {
            return Optional.of(number.longValue());
        }

        if (claimUsuarioId instanceof String text) {
            try {
                return Optional.of(Long.valueOf(text));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * Verifica si la autenticacion contiene una autoridad concreta de Spring Security.
     */
    private boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || authority == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}
