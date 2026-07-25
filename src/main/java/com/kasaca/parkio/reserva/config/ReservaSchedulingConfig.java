package com.kasaca.parkio.reserva.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ReservaSchedulingConfig {

    /**
     * Habilita la ejecucion de tareas programadas del modulo Reserva.
     *
     * <p>Con esta configuracion Spring puede ejecutar metodos anotados con
     * {@code @Scheduled}, como la expiracion automatica de reservas vencidas.</p>
     */
}
