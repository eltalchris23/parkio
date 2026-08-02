package com.kasaca.parkio.pago.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de salida con el detalle del pago registrado.
 */
public record PagoResponse(
        Long id,
        Long ticketId,
        String codigoTicket,
        BigDecimal montoTotal,
        BigDecimal montoRecibido,
        BigDecimal cambio,
        String metodoPago,
        String estado,
        LocalDateTime fechaPago,
        Long operadorId,
        Boolean activo,
        LocalDateTime fechaCreacion
) {
}
