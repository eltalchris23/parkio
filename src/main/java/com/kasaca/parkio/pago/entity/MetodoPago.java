package com.kasaca.parkio.pago.entity;

/**
 * Define los metodos de pago aceptados para liquidar un ticket.
 */
public enum MetodoPago {

    /**
     * Pago recibido en efectivo por el cajero.
     */
    EFECTIVO,

    /**
     * Pago recibido mediante tarjeta bancaria.
     */
    TARJETA,

    /**
     * Pago recibido mediante transferencia bancaria.
     */
    TRANSFERENCIA
}
