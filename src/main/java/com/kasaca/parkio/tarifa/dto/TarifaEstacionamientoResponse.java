package com.kasaca.parkio.tarifa.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de salida para exponer la configuracion de cobro de un estacionamiento.
 */
public record TarifaEstacionamientoResponse(
        Long id,
        Long estacionamientoId,
        BigDecimal precioPorHora,
        Integer minutosTolerancia,
        Boolean cobrarFraccion,
        BigDecimal tarifaMinima,
        Boolean activo,
        LocalDateTime fechaCreacion
) {
}
