package com.kasaca.parkio.catalogo.service;

import com.kasaca.parkio.catalogo.dto.CatalogoResponse;

import java.util.List;

public interface CatalogoService {

    /**
     * Consulta los tipos de cajon disponibles definidos en el enum TipoCajon.
     *
     * @return lista de tipos de cajon para selects o catalogos del frontend
     */
    List<CatalogoResponse> getTiposCajon();

    /**
     * Consulta los estados de cajon disponibles definidos en el enum EstadoCajon.
     *
     * @return lista de estados de cajon para selects o catalogos del frontend
     */
    List<CatalogoResponse> getEstadosCajon();
}
