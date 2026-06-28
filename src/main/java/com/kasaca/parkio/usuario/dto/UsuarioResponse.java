package com.kasaca.parkio.usuario.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record UsuarioResponse(
        Long id,
        String nombre,
        String apellido,
        String email,
        Boolean activo,
        LocalDateTime fechaCreacion,
        Set<String> roles,
        Set<Long> estacionamientoIds
) {
}
