package com.kasaca.parkio.rol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RolRequest(
        @NotBlank(message = "El nombre del rol es obligatorio")
        @Size(
                max = 50,
                message = "El nombre del rol no puede exceder 50 caracteres"
        )
        String nombre,

        @NotNull(message = "El estado activo del rol es obligatorio")
        Boolean activo
) {
}
