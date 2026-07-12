package com.kasaca.parkio.cajon.service;

import com.kasaca.parkio.cajon.dto.CajonRequest;
import com.kasaca.parkio.cajon.dto.CajonResponse;
import com.kasaca.parkio.cajon.dto.CajonEstadoRequest;
import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.entity.TipoCajon;
import com.kasaca.parkio.cajon.mapper.CajonMapper;
import com.kasaca.parkio.cajon.repository.CajonRepository;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.estacionamiento.repository.EstacionamientoRepository;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class CajonServiceImplTest {

    @Mock
    private CajonRepository cajonRepository;

    @Mock
    private EstacionamientoRepository estacionamientoRepository;

    @Mock
    private CajonMapper cajonMapper;

    @InjectMocks
    private CajonServiceImpl cajonService;

    @Test
    void debeObtenerTodosLosCajones() {
        Cajon cajon = crearCajon();
        CajonResponse response = crearResponse();

        when(cajonRepository.findByActivoTrue()).thenReturn(List.of(cajon));
        when(cajonMapper.toResponseCajon(cajon)).thenReturn(response);

        List<CajonResponse> resultado = cajonService.getCajones();

        assertThat(resultado).containsExactly(response);
        verify(cajonRepository).findByActivoTrue();
        verify(cajonMapper).toResponseCajon(cajon);
    }

    @Test
    void debeObtenerCajonesPorEstacionamiento() {
        Estacionamiento estacionamiento = crearEstacionamiento();
        Cajon cajon = crearCajon();
        CajonResponse response = crearResponse();

        when(estacionamientoRepository.findByIdAndActivoTrue(10L))
                .thenReturn(Optional.of(estacionamiento));
        when(cajonRepository.findByEstacionamientoIdAndActivoTrue(10L))
                .thenReturn(List.of(cajon));
        when(cajonMapper.toResponseCajon(cajon)).thenReturn(response);

        List<CajonResponse> resultado =
                cajonService.getCajonesByEstacionamientoId(10L);

        assertThat(resultado).containsExactly(response);
    }

    @Test
    void debeRechazarListadoCuandoEstacionamientoNoExiste() {
        when(estacionamientoRepository.findByIdAndActivoTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                cajonService.getCajonesByEstacionamientoId(99L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Estacionamiento con identificador '99' no fue encontrado"
                );

        verify(cajonRepository, never())
                .findByEstacionamientoIdAndActivoTrue(any());
    }

    @Test
    void debeObtenerCajonPorId() {
        Cajon cajon = crearCajon();
        CajonResponse response = crearResponse();

        when(cajonRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(cajon));
        when(cajonMapper.toResponseCajon(cajon)).thenReturn(response);

        CajonResponse resultado = cajonService.getCajon(1L);

        assertThat(resultado).isEqualTo(response);
    }

    @Test
    void debeRechazarConsultaCuandoCajonNoExiste() {
        when(cajonRepository.findByIdAndActivoTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cajonService.getCajon(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Cajón con identificador '99' no fue encontrado"
                );

        verify(cajonMapper, never()).toResponseCajon(any());
    }

    @Test
    void debeCrearCajon() {
        CajonRequest request = crearRequest();
        Estacionamiento estacionamiento = crearEstacionamiento();
        Cajon cajon = crearCajon();
        CajonResponse response = crearResponse();

        when(estacionamientoRepository.findByIdAndActivoTrue(10L))
                .thenReturn(Optional.of(estacionamiento));
        when(cajonRepository.existsByEstacionamientoIdAndNumero(
                10L,
                "A-001"
        )).thenReturn(false);
        when(cajonMapper.toEntity(request, estacionamiento))
                .thenReturn(cajon);
        when(cajonRepository.save(cajon)).thenReturn(cajon);
        when(cajonMapper.toResponseCajon(cajon)).thenReturn(response);

        CajonResponse resultado = cajonService.addCajon(request);

        assertThat(resultado).isEqualTo(response);
        verify(cajonRepository).save(cajon);
    }

    @Test
    void debeRechazarCreacionCuandoNumeroEstaDuplicado() {
        CajonRequest request = crearRequest();

        when(estacionamientoRepository.findByIdAndActivoTrue(10L))
                .thenReturn(Optional.of(crearEstacionamiento()));
        when(cajonRepository.existsByEstacionamientoIdAndNumero(
                10L,
                "A-001"
        )).thenReturn(true);

        assertThatThrownBy(() -> cajonService.addCajon(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage(
                        "Ya existe el cajón 'A-001' en el estacionamiento '10'"
                );

        verify(cajonRepository, never()).save(any());
    }

    @Test
    void debeActualizarCajonConservandoEntidadExistente() {
        CajonRequest request = new CajonRequest(
                "B-002",
                TipoCajon.ELECTRICO,
                10L
        );
        Cajon cajon = crearCajon();
        Estacionamiento estacionamiento = crearEstacionamiento();
        CajonResponse response = new CajonResponse(
                1L,
                "B-002",
                TipoCajon.ELECTRICO,
                EstadoCajon.LIBRE,
                10L,
                true,
                cajon.getFechaCreacion()
        );

        when(cajonRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(cajon));
        when(estacionamientoRepository.findByIdAndActivoTrue(10L))
                .thenReturn(Optional.of(estacionamiento));
        when(cajonRepository
                .existsByEstacionamientoIdAndNumeroAndIdNot(
                        10L,
                        "B-002",
                        1L
                )).thenReturn(false);
        when(cajonRepository.save(cajon)).thenReturn(cajon);
        when(cajonMapper.toResponseCajon(cajon)).thenReturn(response);

        CajonResponse resultado = cajonService.updateCajon(1L, request);

        assertThat(resultado).isEqualTo(response);
        verify(cajonMapper).updateEntity(
                request,
                cajon,
                estacionamiento
        );
        verify(cajonRepository).save(cajon);
    }

    @Test
    void debeRechazarActualizacionCuandoNumeroEstaDuplicado() {
        CajonRequest request = crearRequest();
        Cajon cajon = crearCajon();

        when(cajonRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(cajon));
        when(estacionamientoRepository.findByIdAndActivoTrue(10L))
                .thenReturn(Optional.of(crearEstacionamiento()));
        when(cajonRepository
                .existsByEstacionamientoIdAndNumeroAndIdNot(
                        10L,
                        "A-001",
                        1L
                )).thenReturn(true);

        assertThatThrownBy(() -> cajonService.updateCajon(1L, request))
                .isInstanceOf(ConflictException.class);

        verify(cajonMapper, never()).updateEntity(any(), any(), any());
        verify(cajonRepository, never()).save(any());
    }

    @Test
    void debeActualizarEstadoDelCajon() {
        Cajon cajon = crearCajon();
        CajonEstadoRequest request = new CajonEstadoRequest(
                EstadoCajon.OCUPADO
        );
        CajonResponse response = new CajonResponse(
                1L,
                "A-001",
                TipoCajon.AUTO,
                EstadoCajon.OCUPADO,
                10L,
                true,
                cajon.getFechaCreacion()
        );

        when(cajonRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(cajon));
        when(cajonRepository.save(cajon)).thenReturn(cajon);
        when(cajonMapper.toResponseCajon(cajon)).thenReturn(response);

        CajonResponse resultado = cajonService.updateEstado(1L, request);

        assertThat(cajon.getEstado()).isEqualTo(EstadoCajon.OCUPADO);
        assertThat(resultado).isEqualTo(response);
        verify(cajonRepository).save(cajon);
    }

    @Test
    void debeRechazarCambioDeEstadoCuandoCajonNoExiste() {
        CajonEstadoRequest request = new CajonEstadoRequest(
                EstadoCajon.FUERA_SERVICIO
        );

        when(cajonRepository.findByIdAndActivoTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                cajonService.updateEstado(99L, request)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Cajón con identificador '99' no fue encontrado"
                );

        verify(cajonRepository, never()).save(any());
    }

    @Test
    void debeEliminarCajonLogicamente() {
        Cajon cajon = crearCajon();

        when(cajonRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(cajon));
        when(cajonRepository.save(cajon)).thenReturn(cajon);

        cajonService.deleteCajon(1L);

        assertThat(cajon.getActivo()).isFalse();
        verify(cajonRepository).save(cajon);
    }

    @Test
    void debeRechazarEliminacionCuandoCajonNoExiste() {
        when(cajonRepository.findByIdAndActivoTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cajonService.deleteCajon(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(
                        "Cajón con identificador '99' no fue encontrado"
                );

        verify(cajonRepository, never()).save(any());
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

    private CajonResponse crearResponse() {
        return new CajonResponse(
                1L,
                "A-001",
                TipoCajon.AUTO,
                EstadoCajon.LIBRE,
                10L,
                true,
                LocalDateTime.of(2026, 6, 27, 12, 0)
        );
    }
}
