package com.kasaca.parkio.estacionamiento.service;

import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoResponse;

import java.util.List;

public interface EstacionamientoService {

    List<EstacionamientoResponse> getEstacionamientos();

    EstacionamientoResponse getEstacionamientoById(Long id);

    EstacionamientoResponse addEstacionamiento(EstacionamientoRequest request);

    EstacionamientoResponse updateEstacionamiento(Long id,EstacionamientoRequest request);

    void deleteEstacionamiento(Long id);
}
