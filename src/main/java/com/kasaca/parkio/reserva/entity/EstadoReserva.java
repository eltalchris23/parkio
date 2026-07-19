package com.kasaca.parkio.reserva.entity;

/**
 * Estados del ciclo de vida de una reserva antes de convertirse en ticket.
 */
public enum EstadoReserva {

    /**
     * La reserva fue generada por el cliente y mantiene apartado el cajon.
     */
    CREADA,

    /**
     * La reserva fue cancelada antes de ser usada.
     */
    CANCELADA,

    /**
     * La reserva vencio por superar su ventana de expiracion.
     *
     * <p>La regla de negocio objetivo es que la creacion de reservas use
     * una expiracion corta, con un maximo de hasta 20 minutos.</p>
     */
    EXPIRADA,

    /**
     * La reserva fue validada en sitio y queda lista para generar un ticket.
     */
    USADA
}
