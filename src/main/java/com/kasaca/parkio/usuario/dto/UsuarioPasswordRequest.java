package com.kasaca.parkio.usuario.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos requeridos para reemplazar la contraseña de un usuario.
 */
public record UsuarioPasswordRequest(

        @NotBlank(message = "La nueva contraseña es obligatoria")
        String nuevaPassword

) {
}
