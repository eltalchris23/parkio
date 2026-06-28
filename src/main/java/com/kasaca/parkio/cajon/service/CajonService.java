package com.kasaca.parkio.cajon.service;

import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.dto.CajonEstadoRequest;

import java.util.List;

public interface CajonService {

    List<CajonResponse> getCajones();

    List<CajonResponse> getCajonesByEstacionamientoId(Long estacionamientoId);

    CajonResponse getCajon(Long id);

    CajonResponse addCajon(CajonRequest request);

    CajonResponse updateCajon(Long id,CajonRequest request);

    CajonResponse updateEstado(
            Long id,
            CajonEstadoRequest request
    );

    void deleteCajon(Long id);
}
