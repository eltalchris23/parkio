package com.kasaca.parkio.estacionamiento.mapper;

import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoResponse;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import org.springframework.stereotype.Component;

@Component
public class EstacionamientoMapper {

    public Estacionamiento toEntity(EstacionamientoRequest request) {
        Estacionamiento estacionamiento = new Estacionamiento();
        updateEntity(request, estacionamiento);
        return estacionamiento;
    }

    public void updateEntity(
            EstacionamientoRequest request,
            Estacionamiento estacionamiento
    ) {
        estacionamiento.setNombre(request.nombre());
        estacionamiento.setDescripcion(request.descripcion());
        estacionamiento.setLatitud(request.latitud());
        estacionamiento.setLongitud(request.longitud());
    }

    public EstacionamientoResponse toResponse(Estacionamiento estacionamiento) {
        return new EstacionamientoResponse(
                estacionamiento.getId(),
                estacionamiento.getNombre(),
                estacionamiento.getDescripcion(),
                estacionamiento.getLatitud(),
                estacionamiento.getLongitud(),
                estacionamiento.getActivo(),
                estacionamiento.getFechaCreacion()
        );
    }
}