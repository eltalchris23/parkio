package com.kasaca.parkio.catalogo.service;

import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.entity.TipoCajon;
import com.kasaca.parkio.catalogo.dto.CatalogoResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class CatalogoServiceImpl implements CatalogoService {

    /**
     * Convierte los valores del enum TipoCajon en opciones simples de catalogo.
     *
     * @return lista con codigo tecnico y descripcion legible de cada tipo de cajon
     */
    @Override
    public List<CatalogoResponse> getTiposCajon() {
        return Arrays.stream(TipoCajon.values())
                .map(tipo -> new CatalogoResponse(tipo.name(), getDescripcionTipoCajon(tipo)))
                .toList();
    }

    /**
     * Convierte los valores del enum EstadoCajon en opciones simples de catalogo.
     *
     * @return lista con codigo tecnico y descripcion legible de cada estado de cajon
     */
    @Override
    public List<CatalogoResponse> getEstadosCajon() {
        return Arrays.stream(EstadoCajon.values())
                .map(estado -> new CatalogoResponse(estado.name(), getDescripcionEstadoCajon(estado)))
                .toList();
    }

    /**
     * Obtiene la descripcion amigable de un tipo de cajon.
     *
     * <p>Se usa un switch explicito para controlar el texto mostrado al frontend
     * y evitar depender de una transformacion automatica del nombre tecnico.</p>
     *
     * @param tipo tipo de cajon definido en el enum
     * @return descripcion legible para mostrar en pantalla
     */
    private String getDescripcionTipoCajon(TipoCajon tipo) {
        return switch (tipo) {
            case AUTO -> "Auto";
            case MOTO -> "Moto";
            case DISCAPACITADO -> "Discapacitado";
            case ELECTRICO -> "Electrico";
        };
    }

    /**
     * Obtiene la descripcion amigable de un estado de cajon.
     *
     * <p>Se mantiene separado de los tipos porque los estados representan el
     * comportamiento operativo del cajon.</p>
     *
     * @param estado estado de cajon definido en el enum
     * @return descripcion legible para mostrar en pantalla
     */
    private String getDescripcionEstadoCajon(EstadoCajon estado) {
        return switch (estado) {
            case LIBRE -> "Libre";
            case RESERVADO -> "Reservado";
            case OCUPADO -> "Ocupado";
            case FUERA_SERVICIO -> "Fuera de servicio";
        };
    }
}
