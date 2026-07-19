package com.kasaca.parkio.cajon.service;

import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.dto.CajonEstadoRequest;
import com.kasaca.parkio.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

public interface CajonService {

    PageResponse<CajonResponse> getCajones(Pageable pageable, Jwt jwt);

    PageResponse<CajonResponse> getCajonesByEstacionamientoId(
            Long estacionamientoId,
            Pageable pageable,
            Jwt jwt
    );

    CajonResponse getCajon(Long id, Jwt jwt);

    CajonResponse addCajon(CajonRequest request, Jwt jwt);

    CajonResponse updateCajon(Long id, CajonRequest request, Jwt jwt);

    CajonResponse updateEstado(
            Long id,
            CajonEstadoRequest request,
            Jwt jwt
    );

    void deleteCajon(Long id, Jwt jwt);
}
