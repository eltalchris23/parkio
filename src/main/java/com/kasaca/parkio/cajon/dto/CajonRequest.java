package com.kasaca.parkio.cajon.dto;

public record CajonRequest(
        String numero,
        String tipo,
        String estado,
        Long estacionamientoId
) {
}
