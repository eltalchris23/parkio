package com.kasaca.parkio.ticket.dto;

import com.kasaca.parkio.ticket.entity.EstadoTicket;

import java.time.LocalDateTime;

/**
 * DTO de salida para devolver la informacion publica de un ticket.
 */
public record TicketResponse(
        Long id,
        String codigo,
        EstadoTicket estado,
        String placa,
        LocalDateTime fechaEntrada,
        LocalDateTime fechaSalida,
        Long reservaId,
        Long usuarioId,
        Long operadorEntradaId,
        Long estacionamientoId,
        Long cajonId,
        Boolean activo,
        LocalDateTime fechaCreacion
) {
}
