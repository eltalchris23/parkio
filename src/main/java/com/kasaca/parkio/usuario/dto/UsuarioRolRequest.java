package com.kasaca.parkio.usuario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Solicitud utilizada para asignar un rol existente a un usuario.
 *
 * @param rolId identificador del rol que se asignará
 */
public record UsuarioRolRequest(

        @NotNull(message = "El identificador del rol es obligatorio")
        @Positive(message = "El identificador del rol debe ser mayor que cero")
        Long rolId

) {
}
