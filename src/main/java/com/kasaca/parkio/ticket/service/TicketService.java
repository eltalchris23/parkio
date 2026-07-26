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
     * @param usuarioAutenticadoId identificador del usuario autenticado tomado desde el JWT.
     * @param request datos necesarios para registrar la entrada.
     * @return ticket generado para la estancia del vehiculo.
     */
    TicketResponse registrarEntrada(Long usuarioAutenticadoId, TicketEntradaRequest request);

    /**
     * Registra la salida de un vehiculo cerrando un ticket abierto.
     *
     * @param usuarioAutenticadoId identificador del usuario autenticado tomado desde el JWT.
     * @param ticketId identificador interno del ticket que se desea cerrar.
     * @return ticket cerrado con fecha de salida.
     */
    TicketResponse registrarSalida(Long usuarioAutenticadoId, Long ticketId);
}
