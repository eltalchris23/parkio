package com.kasaca.parkio.usuario.dto;

public record UsuarioRequest(
        String nombre,
        String apellido,
        String email,
        String password
) {
}
