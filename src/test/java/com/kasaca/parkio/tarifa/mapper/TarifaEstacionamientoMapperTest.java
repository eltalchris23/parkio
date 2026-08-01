package com.kasaca.parkio.tarifa.mapper;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoRequest;
import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoResponse;
import com.kasaca.parkio.tarifa.entity.TarifaEstacionamiento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TarifaEstacionamientoMapperTest {

    private final TarifaEstacionamientoMapper tarifaEstacionamientoMapper =
            new TarifaEstacionamientoMapper();

    /**
     * Verifica que el mapper construya una entidad nueva desde el request y conserve
     * el estacionamiento resuelto previamente por la capa de servicio.
     */
    @Test
    void debeConvertirRequestAEntidad() {
        TarifaEstacionamientoRequest request = crearRequest();
        Estacionamiento estacionamiento = crearEstacionamiento();

        TarifaEstacionamiento tarifa =
                tarifaEstacionamientoMapper.toEntity(request, estacionamiento);

        assertThat(tarifa.getEstacionamiento()).isEqualTo(estacionamiento);
        assertThat(tarifa.getPrecioPorHora()).isEqualByComparingTo("25.00");
        assertThat(tarifa.getMinutosTolerancia()).isEqualTo(10);
        assertThat(tarifa.getCobrarFraccion()).isTrue();
        assertThat(tarifa.getTarifaMinima()).isEqualByComparingTo("15.00");
    }

    /**
     * Verifica que la actualizacion modifique solo los campos editables de la tarifa
     * sin reemplazar la relacion con el estacionamiento.
     */
    @Test
    void debeActualizarEntidadExistente() {
        TarifaEstacionamiento tarifa = crearTarifa();
        Estacionamiento estacionamientoOriginal = tarifa.getEstacionamiento();

        TarifaEstacionamientoRequest request = new TarifaEstacionamientoRequest(
                1L,
                new BigDecimal("30.00"),
                15,
                false,
                new BigDecimal("20.00")
        );

        tarifaEstacionamientoMapper.updateEntity(request, tarifa);

        assertThat(tarifa.getEstacionamiento()).isEqualTo(estacionamientoOriginal);
        assertThat(tarifa.getPrecioPorHora()).isEqualByComparingTo("30.00");
        assertThat(tarifa.getMinutosTolerancia()).isEqualTo(15);
        assertThat(tarifa.getCobrarFraccion()).isFalse();
        assertThat(tarifa.getTarifaMinima()).isEqualByComparingTo("20.00");
    }

    /**
     * Verifica que el mapper exponga un DTO de salida sin serializar la entidad
     * completa de estacionamiento.
     */
    @Test
    void debeConvertirEntidadAResponse() {
        TarifaEstacionamiento tarifa = crearTarifa();

        TarifaEstacionamientoResponse response =
                tarifaEstacionamientoMapper.toResponse(tarifa);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.estacionamientoId()).isEqualTo(1L);
        assertThat(response.precioPorHora()).isEqualByComparingTo("25.00");
        assertThat(response.minutosTolerancia()).isEqualTo(10);
        assertThat(response.cobrarFraccion()).isTrue();
        assertThat(response.tarifaMinima()).isEqualByComparingTo("15.00");
        assertThat(response.activo()).isTrue();
        assertThat(response.fechaCreacion()).isEqualTo(tarifa.getFechaCreacion());
    }

    /**
     * Construye un request valido para reutilizarlo en las pruebas del mapper.
     */
    private TarifaEstacionamientoRequest crearRequest() {
        return new TarifaEstacionamientoRequest(
                1L,
                new BigDecimal("25.00"),
                10,
                true,
                new BigDecimal("15.00")
        );
    }

    /**
     * Construye un estacionamiento minimo con identificador para simular la relacion JPA.
     */
    private Estacionamiento crearEstacionamiento() {
        Estacionamiento estacionamiento = new Estacionamiento();
        estacionamiento.setId(1L);
        estacionamiento.setNombre("Parkio Centro");
        return estacionamiento;
    }

    /**
     * Construye una tarifa activa con datos completos para validar la conversion a response.
     */
    private TarifaEstacionamiento crearTarifa() {
        TarifaEstacionamiento tarifa = new TarifaEstacionamiento();
        tarifa.setId(3L);
        tarifa.setEstacionamiento(crearEstacionamiento());
        tarifa.setPrecioPorHora(new BigDecimal("25.00"));
        tarifa.setMinutosTolerancia(10);
        tarifa.setCobrarFraccion(true);
        tarifa.setTarifaMinima(new BigDecimal("15.00"));
        tarifa.setActivo(true);
        tarifa.setFechaCreacion(LocalDateTime.of(2026, 8, 1, 12, 0));
        return tarifa;
    }
}
