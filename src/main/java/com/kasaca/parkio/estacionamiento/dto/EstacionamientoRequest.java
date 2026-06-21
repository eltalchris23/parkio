package com.kasaca.parkio.estacionamiento.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EstacionamientoRequest(
        @NotBlank(message = "El nombre del estacionamiento es obligatorio")
        @Size(
                max = 150,
                message = "El nombre del estacionamiento no puede exceder 150 caracteres"
        )
        String nombre,

        @Size(
                max = 500,
                message = "La descripcion del estacionamiento no puede exceder 500 caracteres"
        )
        String descripcion,

        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(
                value = "-90.00000000",
                message = "La latitud debe ser mayor o igual a -90"
        )
        @DecimalMax(
                value = "90.00000000",
                message = "La latitud debe ser menor o igual a 90"
        )
        @Digits(
                integer = 2,
                fraction = 8,
                message = "La latitud debe tener máximo 2 enteros y 8 decimales"
        )
        BigDecimal latitud,

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(
                value = "-180.00000000",
                message = "La longitud debe ser mayor o igual a -180"
        )
        @DecimalMax(
                value = "180.00000000",
                message = "La longitud debe ser menor o igual a 180"
        )
        @Digits(
                integer = 3,
                fraction = 8,
                message = "La longitud debe tener máximo 3 enteros y 8 decimales"
        )
        BigDecimal longitud
) {
}
