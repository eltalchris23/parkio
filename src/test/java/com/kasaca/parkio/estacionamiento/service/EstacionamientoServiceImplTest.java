package com.kasaca.parkio.estacionamiento.service;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.cajon.repository.CajonRepository;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoRequest;
import com.kasaca.parkio.estacionamiento.dto.EstacionamientoResponse;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.estacionamiento.mapper.EstacionamientoMapper;
import com.kasaca.parkio.estacionamiento.repository.EstacionamientoRepository;
import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstacionamientoServiceImplTest {

    @Mock
    private EstacionamientoRepository estacionamientoRepository;

    @Mock
    private CajonRepository cajonRepository;

    @Mock
    private EstacionamientoMapper estacionamientoMapper;

    @InjectMocks
    private EstacionamientoServiceImpl estacionamientoService;

    @Test
    void debeObtenerTodosLosEstacionamientos() {
        Estacionamiento estacionamiento = crearEstacionamiento();
        EstacionamientoResponse response = crearResponse();
        Pageable pageable = PageRequest.of(0, 10);

        when(estacionamientoRepository.findByActivoTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(estacionamiento), pageable, 1));
        when(estacionamientoMapper.toResponse(estacionamiento))
                .thenReturn(response);

        PageResponse<EstacionamientoResponse> resultado =
                estacionamientoService.getEstacionamientos(pageable);

        assertThat(resultado.content()).containsExactly(response);
        assertThat(resultado.totalElements()).isEqualTo(1);
        verify(estacionamientoRepository).findByActivoTrue(pageable);
        verify(estacionamientoMapper).toResponse(estacionamiento);
    }

    @Test
    void debeObtenerEstacionamientoPorId() {
        Estacionamiento estacionamiento = crearEstacionamiento();
        EstacionamientoResponse response = crearResponse();

        when(estacionamientoRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(estacionamiento));
        when(estacionamientoMapper.toResponse(estacionamiento))
                .thenReturn(response);

        EstacionamientoResponse resultado =
                estacionamientoService.getEstacionamientoById(1L);

        assertThat(resultado).isEqualTo(response);
    }

    @Test
    void debeLanzarExcepcionCuandoEstacionamientoNoExiste() {
        when(estacionamientoRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                estacionamientoService.getEstacionamientoById(1L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Estacionamiento con identificador '1' no fue encontrado"
                );

        verify(estacionamientoMapper, never())
                .toResponse(any());
    }

    @Test
    void debeCrearEstacionamiento() {
        EstacionamientoRequest request = crearRequest();
        Estacionamiento estacionamiento = crearEstacionamiento();
        EstacionamientoResponse response = crearResponse();

        when(estacionamientoMapper.toEntity(request))
                .thenReturn(estacionamiento);
        when(estacionamientoRepository.save(estacionamiento))
                .thenReturn(estacionamiento);
        when(estacionamientoMapper.toResponse(estacionamiento))
                .thenReturn(response);

        EstacionamientoResponse resultado =
                estacionamientoService.addEstacionamiento(request);

        assertThat(resultado).isEqualTo(response);
        verify(estacionamientoMapper).toEntity(request);
        verify(estacionamientoRepository).save(estacionamiento);
    }

    @Test
    void debeActualizarEstacionamiento() {
        EstacionamientoRequest request = new EstacionamientoRequest(
                "Parkio Reforma",
                "Sucursal Reforma",
                new BigDecimal("19.42700000"),
                new BigDecimal("-99.16770000")
        );

        Estacionamiento estacionamiento = crearEstacionamiento();

        EstacionamientoResponse response =
                new EstacionamientoResponse(
                        1L,
                        "Parkio Reforma",
                        "Sucursal Reforma",
                        request.latitud(),
                        request.longitud(),
                        true,
                        estacionamiento.getFechaCreacion()
                );

        when(estacionamientoRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(estacionamiento));
        when(estacionamientoRepository.save(estacionamiento))
                .thenReturn(estacionamiento);
        when(estacionamientoMapper.toResponse(estacionamiento))
                .thenReturn(response);

        EstacionamientoResponse resultado =
                estacionamientoService.updateEstacionamiento(
                        1L,
                        request
                );

        assertThat(resultado).isEqualTo(response);
        verify(estacionamientoMapper)
                .updateEntity(request, estacionamiento);
        verify(estacionamientoRepository).save(estacionamiento);
    }

    @Test
    void debeRechazarActualizacionCuandoEstacionamientoNoExiste() {
        EstacionamientoRequest request = crearRequest();

        when(estacionamientoRepository.findByIdAndActivoTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                estacionamientoService.updateEstacionamiento(
                        99L,
                        request
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Estacionamiento con identificador '99' no fue encontrado"
                );

        verify(estacionamientoMapper, never())
                .updateEntity(any(), any());
        verify(estacionamientoRepository, never()).save(any());
    }

    @Test
    void debeEliminarEstacionamientoLogicamenteConSusCajones() {
        Estacionamiento estacionamiento = crearEstacionamiento();
        Cajon cajon = new Cajon();
        cajon.setId(2L);
        cajon.setActivo(true);

        when(estacionamientoRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(estacionamiento));
        when(cajonRepository.findByEstacionamientoIdAndActivoTrue(1L))
                .thenReturn(List.of(cajon));
        when(cajonRepository.saveAll(List.of(cajon)))
                .thenReturn(List.of(cajon));
        when(estacionamientoRepository.save(estacionamiento))
                .thenReturn(estacionamiento);

        estacionamientoService.deleteEstacionamiento(1L);

        assertThat(estacionamiento.getActivo()).isFalse();
        assertThat(cajon.getActivo()).isFalse();
        verify(cajonRepository).saveAll(List.of(cajon));
        verify(estacionamientoRepository).save(estacionamiento);
    }

    @Test
    void debeRechazarEliminacionCuandoEstacionamientoNoExiste() {
        when(estacionamientoRepository.findByIdAndActivoTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                estacionamientoService.deleteEstacionamiento(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Estacionamiento con identificador '99' no fue encontrado"
                );

        verify(estacionamientoRepository, never()).save(any());
        verify(cajonRepository, never()).saveAll(any());
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

    private EstacionamientoResponse crearResponse() {
        return new EstacionamientoResponse(
                1L,
                "Parkio Centro",
                "Sucursal Centro Histórico",
                new BigDecimal("19.43260800"),
                new BigDecimal("-99.13320900"),
                true,
                LocalDateTime.of(2026, 6, 21, 12, 0)
        );
    }
}
