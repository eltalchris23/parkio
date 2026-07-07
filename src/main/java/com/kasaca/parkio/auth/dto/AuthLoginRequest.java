package com.kasaca.parkio.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de autenticacion para iniciar sesion en Parkio.
 *
 * <p>Contiene las credenciales minimas necesarias para validar a un usuario:
 * correo electronico y contrasena en texto plano enviada por el cliente.</p>
 *
 * <p>La contrasena solo se usa durante la validacion y nunca debe almacenarse,
 * registrarse en logs ni devolverse en respuestas.</p>
 */
public record AuthLoginRequest(

        @NotBlank(message = "El correo electronico es obligatorio")
        @Email(message = "El correo electronico debe tener un formato valido")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {
}
