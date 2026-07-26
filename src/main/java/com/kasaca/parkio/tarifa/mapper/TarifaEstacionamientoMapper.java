package com.kasaca.parkio.tarifa.mapper;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoRequest;
import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoResponse;
import com.kasaca.parkio.tarifa.entity.TarifaEstacionamiento;
import org.springframework.stereotype.Component;

/**
 * Mapper encargado de convertir entre DTOs y la entidad TarifaEstacionamiento.
 */
@Component
public class TarifaEstacionamientoMapper {

    /**
     * Convierte un DTO de entrada en una entidad nueva de tarifa.
     *
     * <p>El estacionamiento se recibe ya resuelto desde el service porque el frontend
     * solo debe enviar el identificador, no una entidad completa.</p>
     */
    public TarifaEstacionamiento toEntity(TarifaEstacionamientoRequest request, Estacionamiento estacionamiento) {
        TarifaEstacionamiento tarifa = new TarifaEstacionamiento();
        updateEntity(request, tarifa);
        tarifa.setEstacionamiento(estacionamiento);
        return tarifa;
    }

    /**
     * Actualiza los datos editables de una tarifa existente.
     *
     * <p>No cambia el estacionamiento asociado para evitar mover una tarifa de un
     * estacionamiento a otro durante una actualizacion normal.</p>
     */
    public void updateEntity(TarifaEstacionamientoRequest request, TarifaEstacionamiento tarifa) {
        tarifa.setPrecioPorHora(request.precioPorHora());
        tarifa.setMinutosTolerancia(request.minutosTolerancia());
        tarifa.setCobrarFraccion(request.cobrarFraccion());
        tarifa.setTarifaMinima(request.tarifaMinima());
    }

    /**
     * Convierte la entidad TarifaEstacionamiento en el DTO de salida de la API.
     *
     * <p>Solo expone el id del estacionamiento para evitar serializar relaciones JPA
     * completas en la respuesta HTTP.</p>
     */
    public TarifaEstacionamientoResponse toResponse(TarifaEstacionamiento tarifa) {
        return new TarifaEstacionamientoResponse(
                tarifa.getId(),
                tarifa.getEstacionamiento().getId(),
                tarifa.getPrecioPorHora(),
                tarifa.getMinutosTolerancia(),
                tarifa.getCobrarFraccion(),
                tarifa.getTarifaMinima(),
                tarifa.getActivo(),
                tarifa.getFechaCreacion()
        );
    }
}
