package com.kasaca.parkio.reserva.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion Spring del modulo Reserva.
 *
 * <p>Registra las propiedades funcionales del modulo para que puedan inyectarse
 * en servicios futuros sin leer valores directamente desde archivos YAML.</p>
 */
@Configuration
@EnableConfigurationProperties(ReservaProperties.class)
public class ReservaConfig {
}
