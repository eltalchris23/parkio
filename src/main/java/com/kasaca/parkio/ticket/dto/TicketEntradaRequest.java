package com.kasaca.parkio.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para registrar la entrada de un vehiculo al estacionamiento.
 *
 * <p>El operador solo envia el codigo publico de la reserva. El backend obtiene
 * el operador desde el JWT, valida la reserva y genera el ticket.</p>
 */
public record TicketEntradaRequest(

        @NotBlank(message = "El codigo de reserva es obligatorio")
        @Size(max = 30, message = "El codigo de reserva no puede exceder 30 caracteres")
        String codigoReserva
) {
}
