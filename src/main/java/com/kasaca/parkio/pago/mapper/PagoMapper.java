package com.kasaca.parkio.pago.mapper;

import com.kasaca.parkio.pago.dto.PagoResponse;
import com.kasaca.parkio.pago.entity.EstadoPago;
import com.kasaca.parkio.pago.entity.MetodoPago;
import com.kasaca.parkio.pago.entity.Pago;
import com.kasaca.parkio.ticket.entity.Ticket;
import com.kasaca.parkio.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mapper encargado de convertir entidades Pago a DTOs de salida
 * y apoyar la creacion de pagos desde datos controlados por el service.
 */
@Component
public class PagoMapper {

    /**
     * Construye una entidad Pago nueva con los importes ya validados y calculados.
     *
     * <p>El monto total se toma del ticket para evitar que el frontend pueda alterar
     * el cobro calculado previamente durante la salida.</p>
     */
    public Pago toEntity(
            Ticket ticket,
            Usuario operador,
            BigDecimal montoRecibido,
            BigDecimal cambio,
            MetodoPago metodoPago,
            LocalDateTime fechaPago
    ) {
        return Pago.builder()
                .ticket(ticket)
                .operador(operador)
                .montoTotal(ticket.getMontoTotal())
                .montoRecibido(montoRecibido)
                .cambio(cambio)
                .metodoPago(metodoPago)
                .estado(EstadoPago.REGISTRADO)
                .fechaPago(fechaPago)
                .build();
    }

    /**
     * Convierte una entidad Pago en el DTO de respuesta de la API.
     *
     * <p>Solo expone identificadores y datos de negocio necesarios, evitando
     * serializar las relaciones JPA completas.</p>
     */
    public PagoResponse toResponse(Pago pago) {
        return new PagoResponse(
                pago.getId(),
                pago.getTicket().getId(),
                pago.getTicket().getCodigo(),
                pago.getMontoTotal(),
                pago.getMontoRecibido(),
                pago.getCambio(),
                pago.getMetodoPago().name(),
                pago.getEstado().name(),
                pago.getFechaPago(),
                pago.getOperador().getId(),
                pago.getActivo(),
                pago.getFechaCreacion()
        );
    }
}
