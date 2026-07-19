package com.kasaca.parkio.estacionamiento.mapper;

import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoResponse;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EstacionamientoMapperTest {

    private final EstacionamientoMapper estacionamientoMapper =
            new EstacionamientoMapper();

    @Test
    void debeConvertirRequestAEntidad() {
        EstacionamientoRequest request = crearRequest();

        Estacionamiento estacionamiento =
                estacionamientoMapper.toEntity(request, null);

        assertThat(estacionamiento.getNombre())
                .isEqualTo("Parkio Centro");
        assertThat(estacionamiento.getDescripcion())
                .isEqualTo("Sucursal Centro Histórico");
        assertThat(estacionamiento.getLatitud())
                .isEqualByComparingTo("19.43260800");
        assertThat(estacionamiento.getLongitud())
                .isEqualByComparingTo("-99.13320900");
        assertThat(estacionamiento.getActivo()).isTrue();
        assertThat(estacionamiento.getOwner()).isNull();
        assertThat(estacionamiento.getCajones()).isEmpty();
        assertThat(estacionamiento.getUsuarios()).isEmpty();
    }

    @Test
    void debeConvertirRequestAEntidadConOwner() {
        EstacionamientoRequest request = crearRequest();
        Usuario owner = new Usuario();
        owner.setId(7L);

        Estacionamiento estacionamiento =
                estacionamientoMapper.toEntity(request, owner);

        assertThat(estacionamiento.getOwner()).isEqualTo(owner);
        assertThat(estacionamiento.getOwner().getId()).isEqualTo(7L);
    }

    @Test
    void debeActualizarEntidadExistente() {
        Estacionamiento estacionamiento = crearEstacionamiento();

        EstacionamientoRequest request = new EstacionamientoRequest(
                "Parkio Reforma",
                "Sucursal Reforma",
                new BigDecimal("19.42700000"),
                new BigDecimal("-99.16770000")
        );

        estacionamientoMapper.updateEntity(
                request,
                estacionamiento
        );

        assertThat(estacionamiento.getNombre())
                .isEqualTo("Parkio Reforma");
        assertThat(estacionamiento.getDescripcion())
                .isEqualTo("Sucursal Reforma");
        assertThat(estacionamiento.getLatitud())
                .isEqualByComparingTo("19.42700000");
        assertThat(estacionamiento.getLongitud())
                .isEqualByComparingTo("-99.16770000");
        assertThat(estacionamiento.getId()).isEqualTo(1L);
        assertThat(estacionamiento.getActivo()).isTrue();
    }

    @Test
    void debeConvertirEntidadAResponse() {
        Estacionamiento estacionamiento = crearEstacionamiento();

        EstacionamientoResponse response =
                estacionamientoMapper.toResponse(estacionamiento);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("Parkio Centro");
        assertThat(response.descripcion())
                .isEqualTo("Sucursal Centro Histórico");
        assertThat(response.latitud())
                .isEqualByComparingTo("19.43260800");
        assertThat(response.longitud())
                .isEqualByComparingTo("-99.13320900");
        assertThat(response.ownerId()).isNull();
        assertThat(response.activo()).isTrue();
        assertThat(response.fechaCreacion())
                .isEqualTo(estacionamiento.getFechaCreacion());
    }

    private EstacionamientoRequest crearRequest() {
        return new EstacionamientoRequest(
                "Parkio Centro",
                "Sucursal Centro Histórico",
                new BigDecimal("19.43260800"),
                new BigDecimal("-99.13320900")
        );
    }

    private Estacionamiento crearEstacionamiento() {
        Estacionamiento estacionamiento = new Estacionamiento();
        estacionamiento.setId(1L);
        estacionamiento.setNombre("Parkio Centro");
        estacionamiento.setDescripcion("Sucursal Centro Histórico");
        estacionamiento.setLatitud(
                new BigDecimal("19.43260800")
        );
        estacionamiento.setLongitud(
                new BigDecimal("-99.13320900")
        );
        estacionamiento.setActivo(true);
        estacionamiento.setFechaCreacion(
                LocalDateTime.of(2026, 6, 21, 12, 0)
        );
        return estacionamiento;
    }
}
