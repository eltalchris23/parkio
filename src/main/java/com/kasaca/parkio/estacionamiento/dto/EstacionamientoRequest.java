package com.kasaca.parkio.estacionamiento.dto;

import java.math.BigDecimal;

public record EstacionamientoRequest(
        String nombre,
        String descripcion,
        BigDecimal latitud,
        BigDecimal longitud
) {
}
