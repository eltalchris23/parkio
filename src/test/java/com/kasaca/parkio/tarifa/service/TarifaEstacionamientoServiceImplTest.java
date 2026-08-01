package com.kasaca.parkio.tarifa.service;

import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.estacionamiento.repository.EstacionamientoRepository;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoRequest;
import com.kasaca.parkio.tarifa.dto.TarifaEstacionamientoResponse;
import com.kasaca.parkio.tarifa.entity.TarifaEstacionamiento;
import com.kasaca.parkio.tarifa.mapper.TarifaEstacionamientoMapper;
import com.kasaca.parkio.tarifa.repository.TarifaEstacionamientoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TarifaEstacionamientoServiceImplTest {

    @Mock
    private TarifaEstacionamientoRepository tarifaEstacionamientoRepository;

    @Mock
    private EstacionamientoRepository estacionamientoRepository;

    @Mock
    private TarifaEstacionamientoMapper tarifaEstacionamientoMapper;

    @InjectMocks
    private TarifaEstacionamientoServiceImpl tarifaEstacionamientoService;

    /**
     * Verifica que ADMIN pueda consultar una tarifa activa de cualquier estacionamiento.
     */
    @Test
    void debeConsultarTarifaConAdmin() {
        Estacionamiento estacionamiento = crearEstacionamiento();
        TarifaEstacionamiento tarifa = crearTarifa(estacionamiento);
        TarifaEstacionamientoResponse response = crearResponse();

        when(estacionamientoRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(estacionamiento));
        when(tarifaEstacionamientoRepository.findByEstacionamientoIdAndActivoTrue(1L))
                .thenReturn(Optional.of(tarifa));
        when(tarifaEstacionamientoMapper.toResponse(tarifa))
                .thenReturn(response);

        TarifaEstacionamientoResponse resultado =
                tarifaEstacionamientoService.getTarifaByEstacionamientoId(1L, crearJwtAdmin());

        assertThat(resultado).isEqualTo(response);
        verify(estacionamientoRepository).findByIdAndActivoTrue(1L);
        verify(tarifaEstacionamientoMapper).toResponse(tarifa);
    }

    /**
     * Verifica que OWNER pueda crear una tarifa solo cuando el estacionamiento le pertenece.
     */
    @Test
    void debeCrearTarifaConOwner() {
        TarifaEstacionamientoRequest request = crearRequest();
        Estacionamiento estacionamiento = crearEstacionamiento();
        TarifaEstacionamiento tarifa = crearTarifa(estacionamiento);
        TarifaEstacionamientoResponse response = crearResponse();

        when(estacionamientoRepository.findByIdAndOwnerIdAndActivoTrue(1L, 7L))
                .thenReturn(Optional.of(estacionamiento));
        when(tarifaEstacionamientoRepository.existsByEstacionamientoIdAndActivoTrue(1L))
                .thenReturn(false);
        when(tarifaEstacionamientoMapper.toEntity(request, estacionamiento))
                .thenReturn(tarifa);
        when(tarifaEstacionamientoRepository.save(tarifa))
                .thenReturn(tarifa);
        when(tarifaEstacionamientoMapper.toResponse(tarifa))
                .thenReturn(response);

        TarifaEstacionamientoResponse resultado =
                tarifaEstacionamientoService.addTarifa(request, crearJwtOwner());

        assertThat(resultado).isEqualTo(response);
        verify(estacionamientoRepository).findByIdAndOwnerIdAndActivoTrue(1L, 7L);
        verify(tarifaEstacionamientoRepository).save(tarifa);
    }

    /**
     * Verifica que no se pueda crear una segunda tarifa activa para el mismo estacionamiento.
     */
    @Test
    void debeRechazarCreacionCuandoYaExisteTarifaActiva() {
        TarifaEstacionamientoRequest request = crearRequest();
        Estacionamiento estacionamiento = crearEstacionamiento();

        when(estacionamientoRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(estacionamiento));
        when(tarifaEstacionamientoRepository.existsByEstacionamientoIdAndActivoTrue(1L))
                .thenReturn(true);

        assertThatThrownBy(() ->
                tarifaEstacionamientoService.addTarifa(request, crearJwtAdmin())
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage("El estacionamiento con identificador '1' ya tiene una tarifa activa");

        verify(tarifaEstacionamientoMapper, never()).toEntity(any(), any());
        verify(tarifaEstacionamientoRepository, never()).save(any());
    }

    /**
     * Verifica que la actualizacion modifique una tarifa activa existente cuando el request es consistente.
     */
    @Test
    void debeActualizarTarifa() {
        TarifaEstacionamientoRequest request = new TarifaEstacionamientoRequest(
                1L,
                new BigDecimal("30.00"),
                15,
                false,
                new BigDecimal("20.00")
        );
        Estacionamiento estacionamiento = crearEstacionamiento();
        TarifaEstacionamiento tarifa = crearTarifa(estacionamiento);
        TarifaEstacionamientoResponse response = new TarifaEstacionamientoResponse(
                3L,
                1L,
                request.precioPorHora(),
                request.minutosTolerancia(),
                request.cobrarFraccion(),
                request.tarifaMinima(),
                true,
                tarifa.getFechaCreacion()
        );

        when(estacionamientoRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(estacionamiento));
        when(tarifaEstacionamientoRepository.findByEstacionamientoIdAndActivoTrue(1L))
                .thenReturn(Optional.of(tarifa));
        when(tarifaEstacionamientoRepository.save(tarifa))
                .thenReturn(tarifa);
        when(tarifaEstacionamientoMapper.toResponse(tarifa))
                .thenReturn(response);

        TarifaEstacionamientoResponse resultado =
                tarifaEstacionamientoService.updateTarifa(1L, request, crearJwtAdmin());

        assertThat(resultado).isEqualTo(response);
        verify(tarifaEstacionamientoMapper).updateEntity(request, tarifa);
        verify(tarifaEstacionamientoRepository).save(tarifa);
    }

    /**
     * Verifica que el service rechace una actualizacion cuando el path y el body apuntan a estacionamientos distintos.
     */
    @Test
    void debeRechazarActualizacionCuandoEstacionamientoNoCoincide() {
        TarifaEstacionamientoRequest request = new TarifaEstacionamientoRequest(
                2L,
                new BigDecimal("30.00"),
                15,
                true,
                new BigDecimal("20.00")
        );

        assertThatThrownBy(() ->
                tarifaEstacionamientoService.updateTarifa(1L, request, crearJwtAdmin())
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage("El estacionamiento del path no coincide con el estacionamiento del cuerpo de la solicitud");

        verify(estacionamientoRepository, never()).findByIdAndActivoTrue(any());
        verify(tarifaEstacionamientoRepository, never()).save(any());
    }

    /**
     * Verifica que la eliminacion logica desactive la tarifa activa sin borrarla fisicamente.
     */
    @Test
    void debeEliminarTarifaLogicamente() {
        Estacionamiento estacionamiento = crearEstacionamiento();
        TarifaEstacionamiento tarifa = crearTarifa(estacionamiento);

        when(estacionamientoRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(estacionamiento));
        when(tarifaEstacionamientoRepository.findByEstacionamientoIdAndActivoTrue(1L))
                .thenReturn(Optional.of(tarifa));
        when(tarifaEstacionamientoRepository.save(tarifa))
                .thenReturn(tarifa);

        tarifaEstacionamientoService.deleteTarifa(1L, crearJwtAdmin());

        assertThat(tarifa.getActivo()).isFalse();
        verify(tarifaEstacionamientoRepository).save(tarifa);
    }

    /**
     * Verifica que OPERADOR no pueda administrar tarifas.
     */
    @Test
    void debeRechazarOperacionConOperador() {
        assertThatThrownBy(() ->
                tarifaEstacionamientoService.getTarifaByEstacionamientoId(1L, crearJwtOperador())
        )
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("No tienes permisos para administrar tarifas");

        verify(estacionamientoRepository, never()).findByIdAndActivoTrue(any());
        verify(tarifaEstacionamientoRepository, never()).findByEstacionamientoIdAndActivoTrue(any());
    }

    /**
     * Verifica que un estacionamiento inexistente se trate como recurso no encontrado.
     */
    @Test
    void debeRechazarCuandoEstacionamientoNoExiste() {
        when(estacionamientoRepository.findByIdAndActivoTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                tarifaEstacionamientoService.getTarifaByEstacionamientoId(99L, crearJwtAdmin())
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Estacionamiento con identificador '99' no fue encontrado");

        verify(tarifaEstacionamientoRepository, never()).findByEstacionamientoIdAndActivoTrue(any());
    }

    /**
     * Construye un request valido para crear o actualizar tarifas.
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
     * Construye un estacionamiento activo usado como relacion de la tarifa.
     */
    private Estacionamiento crearEstacionamiento() {
        Estacionamiento estacionamiento = new Estacionamiento();
        estacionamiento.setId(1L);
        estacionamiento.setNombre("Parkio Centro");
        estacionamiento.setActivo(true);
        return estacionamiento;
    }

    /**
     * Construye una tarifa activa asociada al estacionamiento recibido.
     */
    private TarifaEstacionamiento crearTarifa(Estacionamiento estacionamiento) {
        TarifaEstacionamiento tarifa = new TarifaEstacionamiento();
        tarifa.setId(3L);
        tarifa.setEstacionamiento(estacionamiento);
        tarifa.setPrecioPorHora(new BigDecimal("25.00"));
        tarifa.setMinutosTolerancia(10);
        tarifa.setCobrarFraccion(true);
        tarifa.setTarifaMinima(new BigDecimal("15.00"));
        tarifa.setActivo(true);
        tarifa.setFechaCreacion(LocalDateTime.of(2026, 8, 1, 12, 0));
        return tarifa;
    }

    /**
     * Construye el response esperado para una tarifa activa.
     */
    private TarifaEstacionamientoResponse crearResponse() {
        return new TarifaEstacionamientoResponse(
                3L,
                1L,
                new BigDecimal("25.00"),
                10,
                true,
                new BigDecimal("15.00"),
                true,
                LocalDateTime.of(2026, 8, 1, 12, 0)
        );
    }

    /**
     * Construye un JWT de prueba con rol ADMIN.
     */
    private Jwt crearJwtAdmin() {
        return crearJwt(1L, List.of("ADMIN"));
    }

    /**
     * Construye un JWT de prueba con rol OWNER.
     */
    private Jwt crearJwtOwner() {
        return crearJwt(7L, List.of("OWNER"));
    }

    /**
     * Construye un JWT de prueba con rol OPERADOR.
     */
    private Jwt crearJwtOperador() {
        return crearJwt(8L, List.of("OPERADOR"));
    }

    /**
     * Construye un JWT minimo con el claim usuarioId y la lista de roles.
     */
    private Jwt crearJwt(Long usuarioId, List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claims(claims -> claims.putAll(Map.of(
                        "usuarioId", usuarioId,
                        "roles", roles
                )))
                .build();
    }
}
