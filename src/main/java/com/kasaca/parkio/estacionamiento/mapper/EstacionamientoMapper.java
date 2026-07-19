package com.kasaca.parkio.estacionamiento.mapper;

import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoResponse;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class EstacionamientoMapper {

    /**
     * Convierte el DTO de entrada en una entidad Estacionamiento y asigna su dueño.
     *
     * <p>El owner se recibe desde la capa de servicio porque debe resolverse a
     * partir del usuario autenticado o de una regla administrativa, no desde un
     * dato libre enviado por el frontend.</p>
     */
    public Estacionamiento toEntity(EstacionamientoRequest request, Usuario owner) {
        Estacionamiento estacionamiento = new Estacionamiento();
        updateEntity(request, estacionamiento);
        estacionamiento.setOwner(owner);
        return estacionamiento;
    }

    /**
     * Actualiza los datos editables del estacionamiento.
     *
     * <p>No modifica el owner para evitar cambiar la propiedad del estacionamiento
     * durante una actualización normal.</p>
     */
    public void updateEntity(
            EstacionamientoRequest request,
            Estacionamiento estacionamiento
    ) {
        estacionamiento.setNombre(request.nombre());
        estacionamiento.setDescripcion(request.descripcion());
        estacionamiento.setLatitud(request.latitud());
        estacionamiento.setLongitud(request.longitud());
    }

    /**
     * Convierte la entidad Estacionamiento en el DTO de salida de la API.
     *
     * <p>Si el estacionamiento todavía no tiene owner por tratarse de datos
     * anteriores a la migración, ownerId se devuelve como null.</p>
     */
    public EstacionamientoResponse toResponse(Estacionamiento estacionamiento) {
        return new EstacionamientoResponse(
                estacionamiento.getId(),
                estacionamiento.getNombre(),
                estacionamiento.getDescripcion(),
                estacionamiento.getLatitud(),
                estacionamiento.getLongitud(),
                estacionamiento.getOwner() != null
                        ? estacionamiento.getOwner().getId()
                        : null,
                estacionamiento.getActivo(),
                estacionamiento.getFechaCreacion()
        );
    }
}
