package com.kasaca.parkio.cajon.mapper;

import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import org.springframework.stereotype.Component;

@Component
public class CajonMapper {

    // Pasar de DTO a Entity
    public Cajon toEntity(CajonRequest request, Estacionamiento estacionamiento) {
        Cajon cajon = new Cajon();
        updateEntity(request, cajon, estacionamiento);
        return cajon;
    }

    public void updateEntity(CajonRequest request, Cajon cajon, Estacionamiento estacionamiento) {
        cajon.setNumero(request.numero());
        cajon.setTipo(request.tipo());
        cajon.setEstacionamiento(estacionamiento);
    }

    public CajonResponse toResponseCajon(Cajon cajon) {
        return new CajonResponse(
                cajon.getId(),
                cajon.getNumero(),
                cajon.getTipo(),
                cajon.getEstado(),
                cajon.getEstacionamiento().getId(),
                cajon.getActivo(),
                cajon.getFechaCreacion()
        );
    }
}
