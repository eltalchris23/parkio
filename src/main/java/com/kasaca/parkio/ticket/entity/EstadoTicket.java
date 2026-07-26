package com.kasaca.parkio.ticket.entity;

/**
 * Define los estados principales del ciclo de vida de un ticket.
 */
public enum EstadoTicket {

    /**
     * Indica que el vehiculo ya ingreso al estacionamiento y el ticket sigue activo.
     */
    ABIERTO,

    /**
     * Indica que el vehiculo ya salio del estacionamiento y el ticket fue cerrado.
     */
    CERRADO
}
