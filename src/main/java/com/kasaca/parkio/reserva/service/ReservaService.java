package com.kasaca.parkio.reserva.service;

import com.kasaca.parkio.reserva.dto.ReservaRequest;
import com.kasaca.parkio.reserva.dto.ReservaResponse;
import com.kasaca.parkio.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ReservaService {

    /**
     * Consulta las reservas activas de un usuario autenticado de forma paginada.
     */
    PageResponse<ReservaResponse> getReservasByUsuario(Long usuarioId, Pageable pageable);

    /**
     * Consulta una reserva activa por su identificador interno.
     */
    ReservaResponse getReservaById(Long id);

    /**
     * Consulta una reserva activa por su codigo publico.
     */
    ReservaResponse getReservaByCodigo(String codigo);

    /**
     * Crea una reserva para el usuario autenticado.
     *
     * <p>El frontend solo envia estacionamiento, cajon y placa. El backend calcula
     * codigo, fechas, expiracion y estado inicial.</p>
     */
    ReservaResponse crearReserva(Long usuarioId, ReservaRequest request);

    /**
     * Cancela una reserva activa perteneciente al usuario autenticado.
     *
     * <p>Solo se pueden cancelar reservas en estado CREADA y que no hayan expirado.</p>
     */
    ReservaResponse cancelarReserva(Long reservaId, Long usuarioId);

    /**
     * Expira reservas vencidas y libera sus cajones cuando corresponde.
     *
     * <p>Este metodo queda preparado para ser usado despues por una tarea programada.</p>
     */
    int expirarReservasVencidas();
}
