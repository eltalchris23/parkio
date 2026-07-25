package com.kasaca.parkio.reserva.dto;

import com.kasaca.parkio.reserva.entity.EstadoReserva;

import java.time.LocalDateTime;

/**
 * DTO de salida para devolver la informacion publica de una reserva.
 */
public record ReservaResponse(
        Long id,
        String codigo,
        String placa,
        EstadoReserva estado,
        LocalDateTime fechaReserva,
        LocalDateTime fechaExpiracion,
        Integer tiempoExpiracionMinutos,
        Long usuarioId,
        Long estacionamientoId,
        Long cajonId,
        Boolean activo,
        LocalDateTime fechaCreacion
) {
}
