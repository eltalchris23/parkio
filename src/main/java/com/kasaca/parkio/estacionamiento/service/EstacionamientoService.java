package com.kasaca.parkio.estacionamiento.service;

import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoResponse;
import com.kasaca.parkio.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

public interface EstacionamientoService {

    PageResponse<EstacionamientoResponse> getEstacionamientos(Pageable pageable, Jwt jwt);

    EstacionamientoResponse getEstacionamientoById(Long id, Jwt jwt);

    EstacionamientoResponse addEstacionamiento(EstacionamientoRequest request, Jwt jwt);

    EstacionamientoResponse updateEstacionamiento(Long id, EstacionamientoRequest request, Jwt jwt);

    void deleteEstacionamiento(Long id, Jwt jwt);
}
