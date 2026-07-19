package com.kasaca.parkio.cajon.entity;

/**
 * Estados operativos que puede tener un cajon dentro de un estacionamiento.
 */
public enum EstadoCajon {

    /**
     * El cajon esta disponible para ser consultado, reservado u ocupado.
     */
    LIBRE,

    /**
     * El cajon fue apartado temporalmente por una reserva vigente.
     */
    RESERVADO,

    /**
     * El cajon esta siendo usado por un vehiculo dentro del estacionamiento.
     */
    OCUPADO,

    /**
     * El cajon no puede utilizarse temporalmente por mantenimiento u otra causa operativa.
     */
    FUERA_SERVICIO
}
