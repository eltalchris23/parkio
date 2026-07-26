package com.kasaca.parkio.tarifa.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO de entrada para crear o actualizar la configuracion de cobro de un estacionamiento.
 */
public record TarifaEstacionamientoRequest(

        @NotNull(message = "El identificador del estacionamiento es obligatorio")
        @Min(value = 1, message = "El identificador del estacionamiento debe ser mayor que cero")
        Long estacionamientoId,

        @NotNull(message = "El precio por hora es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio por hora no puede ser negativo")
        BigDecimal precioPorHora,

        @NotNull(message = "Los minutos de tolerancia son obligatorios")
        @Min(value = 0, message = "Los minutos de tolerancia no pueden ser negativos")
        Integer minutosTolerancia,

        @NotNull(message = "El indicador de cobro por fraccion es obligatorio")
        Boolean cobrarFraccion,

        @NotNull(message = "La tarifa minima es obligatoria")
        @DecimalMin(value = "0.00", message = "La tarifa minima no puede ser negativa")
        BigDecimal tarifaMinima
) {
}
