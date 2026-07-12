package com.kasaca.parkio.estacionamiento.service;

import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoResponse;
import com.kasaca.parkio.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface EstacionamientoService {

    PageResponse<EstacionamientoResponse> getEstacionamientos(Pageable pageable);

    EstacionamientoResponse getEstacionamientoById(Long id);

    EstacionamientoResponse addEstacionamiento(EstacionamientoRequest request);

    EstacionamientoResponse updateEstacionamiento(Long id,EstacionamientoRequest request);

    void deleteEstacionamiento(Long id);
}
