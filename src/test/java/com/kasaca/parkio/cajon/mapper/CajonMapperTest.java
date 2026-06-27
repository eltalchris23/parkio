package com.kasaca.parkio.cajon.mapper;

import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.entity.TipoCajon;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CajonMapperTest {

    private final CajonMapper cajonMapper = new CajonMapper();

    @Test
    void debeConvertirRequestAEntidad() {
        CajonRequest request = crearRequest();
        Estacionamiento estacionamiento = crearEstacionamiento();

        Cajon cajon = cajonMapper.toEntity(request, estacionamiento);

        assertThat(cajon.getNumero()).isEqualTo("A-001");
        assertThat(cajon.getTipo()).isEqualTo(TipoCajon.AUTO);
        assertThat(cajon.getEstado()).isEqualTo(EstadoCajon.LIBRE);
        assertThat(cajon.getEstacionamiento()).isSameAs(estacionamiento);
        assertThat(cajon.getActivo()).isTrue();
    }

    @Test
    void debeActualizarEntidadSinModificarEstadoNiIdentificador() {
        Cajon cajon = crearCajon();
        cajon.setEstado(EstadoCajon.OCUPADO);

        Estacionamiento nuevoEstacionamiento = new Estacionamiento();
        nuevoEstacionamiento.setId(2L);

        CajonRequest request = new CajonRequest(
                "B-002",
                TipoCajon.ELECTRICO,
                2L
        );

        cajonMapper.updateEntity(
                request,
                cajon,
                nuevoEstacionamiento
        );

        assertThat(cajon.getId()).isEqualTo(1L);
        assertThat(cajon.getNumero()).isEqualTo("B-002");
        assertThat(cajon.getTipo()).isEqualTo(TipoCajon.ELECTRICO);
        assertThat(cajon.getEstado()).isEqualTo(EstadoCajon.OCUPADO);
        assertThat(cajon.getEstacionamiento())
                .isSameAs(nuevoEstacionamiento);
    }

    @Test
    void debeConvertirEntidadAResponse() {
        Cajon cajon = crearCajon();

        CajonResponse response = cajonMapper.toResponseCajon(cajon);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.numero()).isEqualTo("A-001");
        assertThat(response.tipo()).isEqualTo(TipoCajon.AUTO);
        assertThat(response.estado()).isEqualTo(EstadoCajon.LIBRE);
        assertThat(response.estacionamientoId()).isEqualTo(10L);
        assertThat(response.activo()).isTrue();
        assertThat(response.fechaCreacion())
                .isEqualTo(cajon.getFechaCreacion());
    }

    private CajonRequest crearRequest() {
        return new CajonRequest(
                "A-001",
                TipoCajon.AUTO,
                10L
        );
    }

    private Estacionamiento crearEstacionamiento() {
        Estacionamiento estacionamiento = new Estacionamiento();
        estacionamiento.setId(10L);
        return estacionamiento;
    }

    private Cajon crearCajon() {
        Cajon cajon = new Cajon();
        cajon.setId(1L);
        cajon.setNumero("A-001");
        cajon.setTipo(TipoCajon.AUTO);
        cajon.setEstado(EstadoCajon.LIBRE);
        cajon.setEstacionamiento(crearEstacionamiento());
        cajon.setActivo(true);
        cajon.setFechaCreacion(
                LocalDateTime.of(2026, 6, 27, 12, 0)
        );
        return cajon;
    }
}
