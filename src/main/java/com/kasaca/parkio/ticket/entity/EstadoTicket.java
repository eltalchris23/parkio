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
     * Indica que ya se calculo el monto a pagar, pero el pago aun no ha sido registrado.
     *
     * <p>Mientras el ticket esta en este estado, el cajon continua ocupado y se libera
     * hasta que el cajero registre el pago correspondiente.</p>
     */
    PENDIENTE_PAGO,

    /**
     * Indica que el pago fue registrado y el ticket quedo cerrado/liquidado.
     */
    CERRADO
}
