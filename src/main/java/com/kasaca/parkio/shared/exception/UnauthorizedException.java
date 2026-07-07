package com.kasaca.parkio.shared.exception;

/**
 * Excepcion para solicitudes no autenticadas o credenciales invalidas.
 *
 * <p>El manejador global la traduce a una respuesta HTTP 401 Unauthorized.</p>
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * Crea una excepcion de autenticacion con un mensaje seguro para el cliente.
     *
     * @param message mensaje que describe el problema sin revelar detalles sensibles
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
