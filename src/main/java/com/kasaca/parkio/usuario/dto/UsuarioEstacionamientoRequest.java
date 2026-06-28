package com.kasaca.parkio.usuario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Solicitud utilizada para asignar un estacionamiento existente a un usuario.
 *
 * @param estacionamientoId identificador del estacionamiento que se asignará
 */
public record UsuarioEstacionamientoRequest(

        @NotNull(message = "El identificador del estacionamiento es obligatorio")
        @Positive(message = "El identificador del estacionamiento debe ser mayor que cero")
        Long estacionamientoId

) {
}
