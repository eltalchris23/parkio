package com.kasaca.parkio.cajon.service;

import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;

import java.util.List;

public interface CajonService {

    List<CajonResponse> getCajones();

    List<CajonResponse> getCajonesByEstacionamientoId(Long estacionamientoId);

    CajonResponse getCajon(Long id);

    CajonResponse addCajon(CajonRequest request);

    CajonResponse updateCajon(Long id,CajonRequest request);

    void deleteCajon(Long id);
}
