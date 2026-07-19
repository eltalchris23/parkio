package com.kasaca.parkio.estacionamiento.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EstacionamientoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal latitud,
        BigDecimal longitud,
        Long ownerId,
        Boolean activo,
        LocalDateTime fechaCreacion
) {
}
