package com.kasaca.parkio.ticket.service;

import com.kasaca.parkio.ticket.dto.TicketEntradaRequest;
import com.kasaca.parkio.ticket.dto.TicketResponse;

/**
 * Contrato de negocio para operaciones del modulo Ticket.
 */
public interface TicketService {

    /**
     * Registra la entrada de un vehiculo convirtiendo una reserva vigente en ticket.
     *
     * @param operadorId identificador del operador autenticado tomado desde el JWT.
     * @param request datos necesarios para registrar la entrada.
     * @return ticket generado para la estancia del vehiculo.
     */
    TicketResponse registrarEntrada(Long operadorId, TicketEntradaRequest request);
}
