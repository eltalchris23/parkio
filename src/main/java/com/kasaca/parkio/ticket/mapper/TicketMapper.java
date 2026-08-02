package com.kasaca.parkio.ticket.mapper;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.reserva.entity.Reserva;
import com.kasaca.parkio.ticket.dto.TicketResponse;
import com.kasaca.parkio.ticket.entity.EstadoTicket;
import com.kasaca.parkio.ticket.entity.Ticket;
import com.kasaca.parkio.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper encargado de convertir entidades Ticket a DTOs de salida
 * y apoyar la creacion inicial de entidades Ticket.
 */
@Component
public class TicketMapper {

    /**
     * Construye una entidad Ticket nueva con los datos calculados por el service.
     *
     * <p>El request no se recibe aqui porque el ticket necesita datos controlados
     * por el backend: codigo, operador autenticado, fecha de entrada, reserva,
     * usuario, estacionamiento, cajon y estado inicial.</p>
     */
    public Ticket toEntity(
            String codigo,
            String placa,
            LocalDateTime fechaEntrada,
            Reserva reserva,
            Usuario usuario,
            Usuario operadorEntrada,
            Estacionamiento estacionamiento,
            Cajon cajon
    ) {
        return Ticket.builder()
                .codigo(codigo)
                .estado(EstadoTicket.ABIERTO)
                .placa(placa)
                .fechaEntrada(fechaEntrada)
                .reserva(reserva)
                .usuario(usuario)
                .operadorEntrada(operadorEntrada)
                .estacionamiento(estacionamiento)
                .cajon(cajon)
                .build();
    }

    /**
     * Convierte una entidad Ticket en el DTO que se devuelve al cliente.
     */
    public TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getCodigo(),
                ticket.getEstado(),
                ticket.getPlaca(),
                ticket.getFechaEntrada(),
                ticket.getFechaSalida(),
                ticket.getMinutosEstancia(),
                ticket.getMontoTotal(),
                ticket.getPrecioPorHoraAplicado(),
                ticket.getMinutosToleranciaAplicados(),
                ticket.getCobrarFraccionAplicado(),
                ticket.getTarifaMinimaAplicada(),
                ticket.getReserva().getId(),
                ticket.getUsuario().getId(),
                ticket.getOperadorEntrada().getId(),
                ticket.getEstacionamiento().getId(),
                ticket.getCajon().getId(),
                ticket.getActivo(),
                ticket.getFechaCreacion()
        );
    }
}
