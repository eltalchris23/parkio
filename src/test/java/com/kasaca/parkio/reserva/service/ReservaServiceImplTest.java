package com.kasaca.parkio.reserva.service;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.entity.TipoCajon;
import com.kasaca.parkio.cajon.repository.CajonRepository;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.estacionamiento.repository.EstacionamientoRepository;
import com.kasaca.parkio.reserva.config.ReservaProperties;
import com.kasaca.parkio.reserva.dto.ReservaRequest;
import com.kasaca.parkio.reserva.dto.ReservaResponse;
import com.kasaca.parkio.reserva.entity.EstadoReserva;
import com.kasaca.parkio.reserva.entity.Reserva;
import com.kasaca.parkio.reserva.mapper.ReservaMapper;
import com.kasaca.parkio.reserva.repository.ReservaRepository;
import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EstacionamientoRepository estacionamientoRepository;

    @Mock
    private CajonRepository cajonRepository;

    @Mock
    private ReservaMapper reservaMapper;

    private ReservaServiceImpl reservaService;

    /**
     * Configura el service con propiedades reales para probar la expiracion parametrizada.
     */
    @BeforeEach
    void setUp() {
        reservaService = new ReservaServiceImpl(
                reservaRepository,
                usuarioRepository,
                estacionamientoRepository,
                cajonRepository,
                reservaMapper,
                new ReservaProperties(20)
        );
    }

    /**
     * Verifica que las reservas de un usuario se devuelvan paginadas.
     */
    @Test
    void debeObtenerReservasPorUsuario() {
        Reserva reserva = crearReserva();
        ReservaResponse response = crearResponse();
        Pageable pageable = PageRequest.of(0, 10);

        when(reservaRepository.findByUsuarioIdAndActivoTrue(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(reserva), pageable, 1));
        when(reservaMapper.toResponse(reserva)).thenReturn(response);

        PageResponse<ReservaResponse> resultado =
                reservaService.getReservasByUsuario(1L, pageable);

        assertThat(resultado.content()).containsExactly(response);
        assertThat(resultado.totalElements()).isEqualTo(1);
        verify(reservaRepository).findByUsuarioIdAndActivoTrue(1L, pageable);
    }

    /**
     * Verifica que una reserva activa pueda consultarse por ID.
     */
    @Test
    void debeObtenerReservaPorId() {
        Reserva reserva = crearReserva();
        ReservaResponse response = crearResponse();

        when(reservaRepository.findByIdAndActivoTrue(30L))
                .thenReturn(Optional.of(reserva));
        when(reservaMapper.toResponse(reserva)).thenReturn(response);

        ReservaResponse resultado = reservaService.getReservaById(30L);

        assertThat(resultado).isEqualTo(response);
    }

    /**
     * Verifica que se responda 404 funcional cuando la reserva por ID no existe.
     */
    @Test
    void debeRechazarConsultaPorIdCuandoReservaNoExiste() {
        when(reservaRepository.findByIdAndActivoTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.getReservaById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Reserva con identificador '99' no fue encontrado");

        verify(reservaMapper, never()).toResponse(any());
    }

    /**
     * Verifica que una reserva activa pueda consultarse por codigo publico.
     */
    @Test
    void debeObtenerReservaPorCodigo() {
        Reserva reserva = crearReserva();
        ReservaResponse response = crearResponse();

        when(reservaRepository.findByCodigoAndActivoTrue("RSV-ABC12345"))
                .thenReturn(Optional.of(reserva));
        when(reservaMapper.toResponse(reserva)).thenReturn(response);

        ReservaResponse resultado =
                reservaService.getReservaByCodigo("RSV-ABC12345");

        assertThat(resultado).isEqualTo(response);
    }

    /**
     * Verifica el flujo exitoso de creacion de reserva y el cambio del cajon a RESERVADO.
     */
    @Test
    void debeCrearReservaYCambiarCajonAReservado() {
        ReservaRequest request = crearRequest();
        Usuario usuario = crearUsuario();
        Estacionamiento estacionamiento = crearEstacionamiento();
        Cajon cajon = crearCajon(estacionamiento, EstadoCajon.LIBRE);
        Reserva reserva = crearReserva(usuario, estacionamiento, cajon);
        ReservaResponse response = crearResponse();

        when(usuarioRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(usuario));
        when(estacionamientoRepository.findByIdAndActivoTrue(10L))
                .thenReturn(Optional.of(estacionamiento));
        when(cajonRepository.findByIdAndActivoTrue(20L))
                .thenReturn(Optional.of(cajon));
        when(reservaRepository.existsByCajonIdAndEstadoAndFechaExpiracionAfterAndActivoTrue(
                eq(20L),
                eq(EstadoReserva.CREADA),
                any(LocalDateTime.class)
        )).thenReturn(false);
        when(reservaRepository.findByCodigoAndActivoTrue(anyString()))
                .thenReturn(Optional.empty());
        when(reservaMapper.toEntity(
                anyString(),
                eq("ABC123"),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(20),
                eq(usuario),
                eq(estacionamiento),
                eq(cajon)
        )).thenReturn(reserva);
        when(cajonRepository.save(cajon)).thenReturn(cajon);
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(reservaMapper.toResponse(reserva)).thenReturn(response);

        ReservaResponse resultado = reservaService.crearReserva(1L, request);

        assertThat(resultado).isEqualTo(response);
        assertThat(cajon.getEstado()).isEqualTo(EstadoCajon.RESERVADO);
        verify(cajonRepository).save(cajon);
        verify(reservaRepository).save(reserva);
    }

    /**
     * Verifica que no se cree reserva cuando el usuario autenticado no existe o esta inactivo.
     */
    @Test
    void debeRechazarCreacionCuandoUsuarioNoExiste() {
        when(usuarioRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.crearReserva(1L, crearRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuario con identificador '1' no fue encontrado");

        verify(reservaRepository, never()).save(any());
    }

    /**
     * Verifica que no se cree reserva cuando el estacionamiento indicado no existe.
     */
    @Test
    void debeRechazarCreacionCuandoEstacionamientoNoExiste() {
        when(usuarioRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(crearUsuario()));
        when(estacionamientoRepository.findByIdAndActivoTrue(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.crearReserva(1L, crearRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Estacionamiento con identificador '10' no fue encontrado");

        verify(reservaRepository, never()).save(any());
    }

    /**
     * Verifica que no se cree reserva cuando el cajon indicado no existe.
     */
    @Test
    void debeRechazarCreacionCuandoCajonNoExiste() {
        when(usuarioRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(crearUsuario()));
        when(estacionamientoRepository.findByIdAndActivoTrue(10L))
                .thenReturn(Optional.of(crearEstacionamiento()));
        when(cajonRepository.findByIdAndActivoTrue(20L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.crearReserva(1L, crearRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cajon con identificador '20' no fue encontrado");

        verify(reservaRepository, never()).save(any());
    }

    /**
     * Verifica que no se cree reserva si el cajon no pertenece al estacionamiento solicitado.
     */
    @Test
    void debeRechazarCreacionCuandoCajonNoPerteneceAlEstacionamiento() {
        Estacionamiento estacionamiento = crearEstacionamiento();
        Estacionamiento otroEstacionamiento = new Estacionamiento();
        otroEstacionamiento.setId(99L);
        Cajon cajon = crearCajon(otroEstacionamiento, EstadoCajon.LIBRE);

        when(usuarioRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(crearUsuario()));
        when(estacionamientoRepository.findByIdAndActivoTrue(10L))
                .thenReturn(Optional.of(estacionamiento));
        when(cajonRepository.findByIdAndActivoTrue(20L))
                .thenReturn(Optional.of(cajon));

        assertThatThrownBy(() -> reservaService.crearReserva(1L, crearRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El cajon no pertenece al estacionamiento indicado.");

        verify(reservaRepository, never()).save(any());
    }

    /**
     * Verifica que no se cree reserva si el cajon esta ocupado, reservado o fuera de servicio.
     */
    @Test
    void debeRechazarCreacionCuandoCajonNoEstaLibre() {
        Estacionamiento estacionamiento = crearEstacionamiento();
        Cajon cajon = crearCajon(estacionamiento, EstadoCajon.OCUPADO);

        when(usuarioRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(crearUsuario()));
        when(estacionamientoRepository.findByIdAndActivoTrue(10L))
                .thenReturn(Optional.of(estacionamiento));
        when(cajonRepository.findByIdAndActivoTrue(20L))
                .thenReturn(Optional.of(cajon));

        assertThatThrownBy(() -> reservaService.crearReserva(1L, crearRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El cajon no esta disponible para reservar.");

        verify(reservaRepository, never()).save(any());
    }

    /**
     * Verifica que no se cree reserva si ya existe una reserva vigente sobre el cajon.
     */
    @Test
    void debeRechazarCreacionCuandoExisteReservaVigente() {
        Estacionamiento estacionamiento = crearEstacionamiento();
        Cajon cajon = crearCajon(estacionamiento, EstadoCajon.LIBRE);

        when(usuarioRepository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(crearUsuario()));
        when(estacionamientoRepository.findByIdAndActivoTrue(10L))
                .thenReturn(Optional.of(estacionamiento));
        when(cajonRepository.findByIdAndActivoTrue(20L))
                .thenReturn(Optional.of(cajon));
        when(reservaRepository.existsByCajonIdAndEstadoAndFechaExpiracionAfterAndActivoTrue(
                eq(20L),
                eq(EstadoReserva.CREADA),
                any(LocalDateTime.class)
        )).thenReturn(true);

        assertThatThrownBy(() -> reservaService.crearReserva(1L, crearRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El cajon ya tiene una reserva vigente.");

        verify(reservaRepository, never()).save(any());
    }

    /**
     * Verifica que las reservas vencidas cambien a EXPIRADA y liberen el cajon reservado.
     */
    @Test
    void debeExpirarReservasVencidasYLiberarCajon() {
        Estacionamiento estacionamiento = crearEstacionamiento();
        Cajon cajon = crearCajon(estacionamiento, EstadoCajon.RESERVADO);
        Reserva reserva = crearReserva(crearUsuario(), estacionamiento, cajon);

        when(reservaRepository.findByEstadoAndFechaExpiracionBeforeAndActivoTrue(
                eq(EstadoReserva.CREADA),
                any(LocalDateTime.class)
        )).thenReturn(List.of(reserva));
        when(reservaRepository.existsByCajonIdAndEstadoAndFechaExpiracionAfterAndActivoTrueAndIdNot(
                eq(20L),
                eq(EstadoReserva.CREADA),
                any(LocalDateTime.class),
                eq(30L)
        )).thenReturn(false);

        int totalExpiradas = reservaService.expirarReservasVencidas();

        assertThat(totalExpiradas).isEqualTo(1);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.EXPIRADA);
        assertThat(cajon.getEstado()).isEqualTo(EstadoCajon.LIBRE);
        verify(cajonRepository).save(cajon);
        verify(reservaRepository).saveAll(List.of(reserva));
    }

    /**
     * Verifica que un usuario pueda cancelar una reserva propia vigente y liberar el cajon.
     */
    @Test
    void debeCancelarReservaPropiaYLiberarCajon() {
        Usuario usuario = crearUsuario();
        Estacionamiento estacionamiento = crearEstacionamiento();
        Cajon cajon = crearCajon(estacionamiento, EstadoCajon.RESERVADO);
        Reserva reserva = crearReserva(usuario, estacionamiento, cajon);
        reserva.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));
        ReservaResponse response = new ReservaResponse(
                30L,
                "RSV-ABC12345",
                "ABC123",
                EstadoReserva.CANCELADA,
                reserva.getFechaReserva(),
                reserva.getFechaExpiracion(),
                20,
                1L,
                10L,
                20L,
                true,
                null
        );

        when(reservaRepository.findByIdAndActivoTrue(30L))
                .thenReturn(Optional.of(reserva));
        when(reservaRepository.existsByCajonIdAndEstadoAndFechaExpiracionAfterAndActivoTrueAndIdNot(
                eq(20L),
                eq(EstadoReserva.CREADA),
                any(LocalDateTime.class),
                eq(30L)
        )).thenReturn(false);
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(reservaMapper.toResponse(reserva)).thenReturn(response);

        ReservaResponse resultado = reservaService.cancelarReserva(30L, 1L);

        assertThat(resultado.estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(cajon.getEstado()).isEqualTo(EstadoCajon.LIBRE);
        verify(cajonRepository).save(cajon);
        verify(reservaRepository).save(reserva);
    }

    /**
     * Verifica que un usuario no pueda cancelar una reserva perteneciente a otro usuario.
     */
    @Test
    void debeRechazarCancelacionCuandoReservaNoPerteneceAlUsuario() {
        Reserva reserva = crearReserva();

        when(reservaRepository.findByIdAndActivoTrue(30L))
                .thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.cancelarReserva(30L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Reserva con identificador '30' no fue encontrado");

        verify(reservaRepository, never()).save(any());
    }

    /**
     * Verifica que una reserva ya cancelada, expirada o usada no pueda cancelarse.
     */
    @Test
    void debeRechazarCancelacionCuandoReservaNoEstaCreada() {
        Reserva reserva = crearReserva();
        reserva.setEstado(EstadoReserva.EXPIRADA);

        when(reservaRepository.findByIdAndActivoTrue(30L))
                .thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.cancelarReserva(30L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Solo se pueden cancelar reservas en estado CREADA.");

        verify(reservaRepository, never()).save(any());
    }

    /**
     * Verifica que una reserva vencida no pueda cancelarse manualmente.
     */
    @Test
    void debeRechazarCancelacionCuandoReservaYaExpiro() {
        Reserva reserva = crearReserva();
        reserva.setFechaExpiracion(LocalDateTime.now().minusMinutes(1));

        when(reservaRepository.findByIdAndActivoTrue(30L))
                .thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.cancelarReserva(30L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("La reserva ya expiro y no puede cancelarse.");

        verify(reservaRepository, never()).save(any());
    }

    /**
     * Crea el request usado para generar una reserva.
     */
    private ReservaRequest crearRequest() {
        return new ReservaRequest(10L, 20L, "ABC123");
    }

    /**
     * Crea un usuario activo de prueba.
     */
    private Usuario crearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Cliente");
        usuario.setEmail("cliente.reserva@parkio.com");
        usuario.setActivo(true);
        return usuario;
    }

    /**
     * Crea un estacionamiento activo de prueba.
     */
    private Estacionamiento crearEstacionamiento() {
        Estacionamiento estacionamiento = new Estacionamiento();
        estacionamiento.setId(10L);
        estacionamiento.setNombre("Estacionamiento Reserva");
        estacionamiento.setActivo(true);
        return estacionamiento;
    }

    /**
     * Crea un cajon de prueba con el estado indicado.
     */
    private Cajon crearCajon(Estacionamiento estacionamiento, EstadoCajon estado) {
        Cajon cajon = new Cajon();
        cajon.setId(20L);
        cajon.setNumero("A-01");
        cajon.setTipo(TipoCajon.AUTO);
        cajon.setEstado(estado);
        cajon.setEstacionamiento(estacionamiento);
        cajon.setActivo(true);
        return cajon;
    }

    /**
     * Crea una reserva completa con relaciones para casos de consulta y expiracion.
     */
    private Reserva crearReserva() {
        return crearReserva(crearUsuario(), crearEstacionamiento(), crearCajon(crearEstacionamiento(), EstadoCajon.LIBRE));
    }

    /**
     * Crea una reserva completa con las relaciones recibidas.
     */
    private Reserva crearReserva(
            Usuario usuario,
            Estacionamiento estacionamiento,
            Cajon cajon
    ) {
        Reserva reserva = Reserva.builder()
                .codigo("RSV-ABC12345")
                .placa("ABC123")
                .estado(EstadoReserva.CREADA)
                .fechaReserva(LocalDateTime.of(2026, 7, 25, 10, 0))
                .fechaExpiracion(LocalDateTime.of(2026, 7, 25, 10, 20))
                .tiempoExpiracionMinutos(20)
                .usuario(usuario)
                .estacionamiento(estacionamiento)
                .cajon(cajon)
                .build();

        reserva.setId(30L);
        reserva.setActivo(true);
        return reserva;
    }

    /**
     * Crea el response esperado de una reserva.
     */
    private ReservaResponse crearResponse() {
        return new ReservaResponse(
                30L,
                "RSV-ABC12345",
                "ABC123",
                EstadoReserva.CREADA,
                LocalDateTime.of(2026, 7, 25, 10, 0),
                LocalDateTime.of(2026, 7, 25, 10, 20),
                20,
                1L,
                10L,
                20L,
                true,
                LocalDateTime.of(2026, 7, 25, 10, 0)
        );
    }
}
