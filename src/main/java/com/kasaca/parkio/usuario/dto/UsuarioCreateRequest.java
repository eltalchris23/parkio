package com.kasaca.parkio.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos requeridos para crear un usuario.
 */
public record UsuarioCreateRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
        String nombre,

        @Size(max = 100, message = "El apellido no puede exceder los 100 caracteres")
        String apellido,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico debe tener un formato válido")
        @Size(max = 150, message = "El correo electrónico no puede exceder los 150 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password

) {
}
