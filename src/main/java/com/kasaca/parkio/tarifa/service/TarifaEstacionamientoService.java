package com.kasaca.parkio.tarifa.service;

import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoRequest;
import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoResponse;
import org.springframework.security.oauth2.jwt.Jwt;

public interface TarifaEstacionamientoService {

    /**
     * Consulta la tarifa activa asociada a un estacionamiento.
     */
    TarifaEstacionamientoResponse getTarifaByEstacionamientoId(Long estacionamientoId, Jwt jwt);

    /**
     * Crea la tarifa activa de un estacionamiento.
     */
    TarifaEstacionamientoResponse addTarifa(TarifaEstacionamientoRequest request, Jwt jwt);

    /**
     * Actualiza la tarifa activa de un estacionamiento.
     */
    TarifaEstacionamientoResponse updateTarifa(Long estacionamientoId, TarifaEstacionamientoRequest request, Jwt jwt);

    /**
     * Desactiva logicamente la tarifa activa de un estacionamiento.
     */
    void deleteTarifa(Long estacionamientoId, Jwt jwt);
}
