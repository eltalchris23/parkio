package com.kasaca.parkio.pago.service;

import com.kasaca.parkio.pago.dto.PagoRequest;
import com.kasaca.parkio.pago.dto.PagoResponse;
import com.kasaca.parkio.pago.entity.MetodoPago;
import com.kasaca.parkio.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Contrato de negocio para registrar y consultar pagos.
 */
public interface PagoService {

    /**
     * Consulta pagos activos de forma paginada aplicando filtros opcionales y alcance por rol.
     */
    PageResponse<PagoResponse> getPagos(
            Long usuarioAutenticadoId,
            Long estacionamientoId,
            MetodoPago metodoPago,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Pageable pageable
    );

    /**
     * Registra el pago de un ticket pendiente y libera el cajon cuando el cobro es valido.
     */
    PagoResponse registrarPago(Long usuarioAutenticadoId, PagoRequest request);

    /**
     * Consulta el pago activo asociado a un ticket validando el alcance del usuario autenticado.
     */
    PagoResponse getPagoByTicketId(Long usuarioAutenticadoId, Long ticketId);
}
