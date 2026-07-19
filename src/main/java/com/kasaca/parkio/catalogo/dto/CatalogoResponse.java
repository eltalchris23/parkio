package com.kasaca.parkio.catalogo.dto;

/**
 * Representa una opcion simple de catalogo para consumo del frontend.
 *
 * @param codigo valor tecnico que el frontend debe enviar al backend
 * @param descripcion texto legible que el frontend puede mostrar al usuario
 */
public record CatalogoResponse(
        String codigo,
        String descripcion
) {
}
