package com.kasaca.parkio.cajon.service;

import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.dto.CajonEstadoRequest;
import com.kasaca.parkio.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CajonService {

    PageResponse<CajonResponse> getCajones(Pageable pageable);

    PageResponse<CajonResponse> getCajonesByEstacionamientoId(
            Long estacionamientoId,
            Pageable pageable
    );

    CajonResponse getCajon(Long id);

    CajonResponse addCajon(CajonRequest request);

    CajonResponse updateCajon(Long id,CajonRequest request);

    CajonResponse updateEstado(
            Long id,
            CajonEstadoRequest request
    );

    void deleteCajon(Long id);
}
