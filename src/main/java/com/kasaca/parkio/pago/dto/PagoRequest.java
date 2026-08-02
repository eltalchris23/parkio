package com.kasaca.parkio.pago.dto;

import com.kasaca.parkio.pago.entity.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO de entrada para registrar el pago de un ticket pendiente.
 */
public record PagoRequest(

        @NotNull(message = "El ticket es obligatorio")
        Long ticketId,

        @NotNull(message = "El monto recibido es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto recibido debe ser mayor a cero")
        BigDecimal montoRecibido,

        @NotNull(message = "El metodo de pago es obligatorio")
        MetodoPago metodoPago
) {
}
