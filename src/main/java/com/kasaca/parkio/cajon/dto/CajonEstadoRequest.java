package com.kasaca.parkio.cajon.dto;

import com.kasaca.parkio.cajon.entity.EstadoCajon;
import jakarta.validation.constraints.NotNull;

public record CajonEstadoRequest(
        @NotNull(message = "El estado del cajón es obligatorio")
        EstadoCajon estado
) {
}
