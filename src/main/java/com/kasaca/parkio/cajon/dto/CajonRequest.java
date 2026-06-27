package com.kasaca.parkio.cajon.dto;

import com.kasaca.parkio.cajon.entity.TipoCajon;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CajonRequest(
        @NotBlank(message = "El número del cajón es obligatorio")
        @Size(
                max = 20,
                message = "El número del cajón no puede exceder 20 caracteres"
        )
        String numero,

        @NotNull(message = "El tipo de cajón es obligatorio")
        TipoCajon tipo,

        @NotNull(message = "El estacionamiento es obligatorio")
        @Positive(
                message = "El identificador del estacionamiento debe ser mayor que cero"
        )
        Long estacionamientoId
) {
}
