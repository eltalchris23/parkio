package com.kasaca.parkio.reserva.mapper;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.reserva.dto.ReservaResponse;
import com.kasaca.parkio.reserva.entity.EstadoReserva;
import com.kasaca.parkio.reserva.entity.Reserva;
import com.kasaca.parkio.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper encargado de convertir entidades Reserva a DTOs de salida
 * y apoyar la creacion inicial de entidades Reserva.
 */
@Component
public class ReservaMapper {

    /**
     * Construye una entidad Reserva nueva con los datos calculados por el service.
     *
     * <p>El request no se recibe aqui porque la reserva necesita varios datos que
     * no deben venir del frontend: usuario autenticado, codigo, fechas, estado
     * inicial y minutos de expiracion configurados.</p>
     */
    public Reserva toEntity(
            String codigo,
            String placa,
            LocalDateTime fechaReserva,
            LocalDateTime fechaExpiracion,
            Integer tiempoExpiracionMinutos,
            Usuario usuario,
            Estacionamiento estacionamiento,
            Cajon cajon
    ) {
        return Reserva.builder()
                .codigo(codigo)
                .placa(placa)
                .estado(EstadoReserva.CREADA)
                .fechaReserva(fechaReserva)
                .fechaExpiracion(fechaExpiracion)
                .tiempoExpiracionMinutos(tiempoExpiracionMinutos)
                .usuario(usuario)
                .estacionamiento(estacionamiento)
                .cajon(cajon)
                .build();
    }

    /**
     * Convierte una entidad Reserva en el DTO que se devuelve al cliente.
     */
    public ReservaResponse toResponse(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getCodigo(),
                reserva.getPlaca(),
                reserva.getEstado(),
                reserva.getFechaReserva(),
                reserva.getFechaExpiracion(),
                reserva.getTiempoExpiracionMinutos(),
                reserva.getUsuario().getId(),
                reserva.getEstacionamiento().getId(),
                reserva.getCajon().getId(),
                reserva.getActivo(),
                reserva.getFechaCreacion()
        );
    }
}
