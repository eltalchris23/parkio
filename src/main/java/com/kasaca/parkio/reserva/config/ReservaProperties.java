package com.kasaca.parkio.reserva.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades configurables del modulo Reserva.
 *
 * <p>Permite ajustar reglas funcionales por ambiente mediante variables de
 * entorno, sin modificar codigo del backend ni del frontend.</p>
 */
@ConfigurationProperties(prefix = "parkio.reserva")
public record ReservaProperties(

        /**
         * Minutos durante los que una reserva nueva permanece vigente antes de expirar.
         */
        Integer expiracionMinutos
) {
}
