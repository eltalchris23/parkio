package com.kasaca.parkio.cajon.dto;

import com.kasaca.parkio.cajon.entity.TipoCajon;

public record CajonRequest(
        String numero,
        TipoCajon tipo,
        Long estacionamientoId
) {
}
