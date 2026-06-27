package com.kasaca.parkio.cajon.dto;

import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.entity.TipoCajon;

import java.time.LocalDateTime;

public record CajonResponse(
        Long id,
        String numero,
        TipoCajon tipo,
        EstadoCajon estado,
        Long estacionamientoId,
        Boolean activo,
        LocalDateTime fechaCreacion
) {
}
