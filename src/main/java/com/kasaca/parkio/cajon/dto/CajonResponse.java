package com.kasaca.parkio.cajon.dto;

import java.time.LocalDateTime;

public record CajonResponse(
        Long id,
        String numero,
        String tipo,
        String estado,
        Long estacionamientoId,
        Boolean activo,
        LocalDateTime fechaCreacion
) {
}
