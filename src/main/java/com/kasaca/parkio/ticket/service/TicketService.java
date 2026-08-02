package com.kasaca.parkio.ticket.service;

import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.ticket.dto.TicketEntradaRequest;
import com.kasaca.parkio.ticket.dto.TicketResponse;
import com.kasaca.parkio.ticket.entity.EstadoTicket;
import org.springframework.data.domain.Pageable;

/**
 * Contrato de negocio para operaciones del modulo Ticket.
 */
public interface TicketService {

    /**
     * Consulta tickets activos de forma paginada segun el alcance del usuario autenticado.
     *
     * @param usuarioAutenticadoId identificador del usuario autenticado tomado desde el JWT.
     * @param estado estado opcional usado para filtrar tickets ABIERTO o CERRADO.
     * @param estacionamientoId identificador opcional del estacionamiento usado como filtro.
     * @param pageable parametros de paginacion y ordenamiento.
     * @return pagina de tickets visibles para el usuario autenticado.
     */
    PageResponse<TicketResponse> getTickets(
            Long usuarioAutenticadoId,
            EstadoTicket estado,
            Long estacionamientoId,
            Pageable pageable
    );

    /**
     * Consulta un ticket activo por identificador validando el alcance del usuario autenticado.
     *
     * @param usuarioAutenticadoId identificador del usuario autenticado tomado desde el JWT.
     * @param ticketId identificador interno del ticket solicitado.
     * @return ticket consultado cuando el usuario tiene permisos para verlo.
     */
    TicketResponse getTicketById(Long usuarioAutenticadoId, Long ticketId);

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
