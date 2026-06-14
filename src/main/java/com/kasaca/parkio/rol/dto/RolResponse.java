package com.kasaca.parkio.rol.dto;

import java.time.LocalDateTime;

public record RolResponse(
        Long id,
        String nombre,
        Boolean activo,
        LocalDateTime fechaCreacion
) {
}
