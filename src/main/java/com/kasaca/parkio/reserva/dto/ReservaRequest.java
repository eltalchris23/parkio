package com.kasaca.parkio.reserva.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear una reserva.
 *
 * <p>El frontend solo envia los datos que puede decidir el cliente.
 * El usuario se obtiene desde el JWT, el codigo lo genera el backend,
 * y la expiracion se calcula usando la configuracion del sistema.</p>
 */
public record ReservaRequest(

        @NotNull(message = "El estacionamiento es obligatorio")
        @Positive(message = "El identificador del estacionamiento debe ser mayor que cero")
        Long estacionamientoId,

        @NotNull(message = "El cajon es obligatorio")
        @Positive(message = "El identificador del cajon debe ser mayor que cero")
        Long cajonId,

        @Size(max = 15, message = "La placa no puede exceder 15 caracteres")
        String placa
) {
}
