package com.kasaca.parkio.ticket.service;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.entity.TipoCajon;
import com.kasaca.parkio.cajon.repository.CajonRepository;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.reserva.entity.EstadoReserva;
import com.kasaca.parkio.reserva.entity.Reserva;
import com.kasaca.parkio.reserva.repository.ReservaRepository;
import com.kasaca.parkio.rol.entity.Rol;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.shared.exception.ResourceNotFoundException;
import com.kasaca.parkio.ticket.dto.TicketEntradaRequest;
import com.kasaca.parkio.ticket.dto.TicketResponse;
import com.kasaca.parkio.ticket.entity.EstadoTicket;
import com.kasaca.parkio.ticket.entity.Ticket;
import com.kasaca.parkio.ticket.mapper.TicketMapper;
import com.kasaca.parkio.ticket.repository.TicketRepository;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CajonRepository cajonRepository;

    @Mock
    private TicketMapper ticketMapper;

    private TicketServiceImpl ticketService;

    /**
     * Configura el service con repositorios y mapper simulados para probar reglas de negocio aisladas.
     */
    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(
                ticketRepository,
                reservaRepository,
                usuarioRepository,
                cajonRepository,
                ticketMapper
        );
    }

    /**
     * Verifica el flujo exitoso: reserva USADA, cajon OCUPADO y ticket ABIERTO.
     */
    @Test
    void debeRegistrarEntradaYCambiarEstados() {
        Estacionamiento estacionamiento = crearEstacionamiento(10L);
        Usuario cliente = crearUsuario(1L, "USER");
        Usuario operador = crearUsuario(2L, "OPERADOR");
        operador.getEstacionamientos().add(estacionamiento);
        Cajon cajon = crearCajon(20L, estacionamiento);
        Reserva reserva = crearReserva(cliente, estacionamiento, cajon, EstadoReserva.CREADA);
        Ticket ticket = crearTicket(40L, reserva, cliente, operador, estacionamiento, cajon);
        TicketResponse response = crearResponse();

        when(usuarioRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(operador));
        when(reservaRepository.findByCodigoAndActivoTrue("RSV-ABC12345")).thenReturn(Optional.of(reserva));
        when(ticketRepository.existsByReservaIdAndActivoTrue(30L)).thenReturn(false);
        when(ticketRepository.existsByCajonIdAndEstadoAndActivoTrue(20L, EstadoTicket.ABIERTO)).thenReturn(false);
        when(ticketRepository.findByCodigoAndActivoTrue(anyString())).thenReturn(Optional.empty());
        when(ticketMapper.toEntity(
                anyString(),
                eq("ABC123"),
                any(LocalDateTime.class),
                eq(reserva),
                eq(cliente),
                eq(operador),
                eq(estacionamiento),
                eq(cajon)
        )).thenReturn(ticket);
        when(cajonRepository.save(cajon)).thenReturn(cajon);
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        TicketResponse resultado =
                ticketService.registrarEntrada(2L, new TicketEntradaRequest("RSV-ABC12345"));

        assertThat(resultado).isEqualTo(response);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.USADA);
        assertThat(cajon.getEstado()).isEqualTo(EstadoCajon.OCUPADO);
        verify(cajonRepository).save(cajon);
        verify(reservaRepository).save(reserva);
        verify(ticketRepository).save(ticket);
    }

    /**
     * Verifica que no se genere ticket cuando el operador no existe o esta inactivo.
     */
    @Test
    void debeRechazarCuandoOperadorNoExiste() {
        when(usuarioRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                ticketService.registrarEntrada(99L, new TicketEntradaRequest("RSV-ABC12345"))
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuario con identificador '99' no fue encontrado");

        verify(ticketRepository, never()).save(any());
    }

    /**
     * Verifica que no se genere ticket cuando el codigo de reserva no existe.
     */
    @Test
    void debeRechazarCuandoReservaNoExiste() {
        Usuario operador = crearUsuario(2L, "OPERADOR");

        when(usuarioRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(operador));
        when(reservaRepository.findByCodigoAndActivoTrue("RSV-NOEXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                ticketService.registrarEntrada(2L, new TicketEntradaRequest("RSV-NOEXISTE"))
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Reserva con identificador 'RSV-NOEXISTE' no fue encontrado");

        verify(ticketRepository, never()).save(any());
    }

    /**
     * Verifica que una reserva cancelada, expirada o usada no pueda convertirse en ticket.
     */
    @Test
    void debeRechazarCuandoReservaNoEstaCreada() {
        Estacionamiento estacionamiento = crearEstacionamiento(10L);
        Usuario cliente = crearUsuario(1L, "USER");
        Usuario operador = crearUsuario(2L, "OPERADOR");
        Cajon cajon = crearCajon(20L, estacionamiento);
        Reserva reserva = crearReserva(cliente, estacionamiento, cajon, EstadoReserva.CANCELADA);

        when(usuarioRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(operador));
        when(reservaRepository.findByCodigoAndActivoTrue("RSV-ABC12345")).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() ->
                ticketService.registrarEntrada(2L, new TicketEntradaRequest("RSV-ABC12345"))
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage("Solo se puede generar ticket para reservas en estado CREADA.");

        verify(ticketRepository, never()).save(any());
    }

    /**
     * Verifica que una reserva vencida no pueda convertirse en ticket.
     */
    @Test
    void debeRechazarCuandoReservaEstaVencida() {
        Estacionamiento estacionamiento = crearEstacionamiento(10L);
        Usuario cliente = crearUsuario(1L, "USER");
        Usuario operador = crearUsuario(2L, "OPERADOR");
        Cajon cajon = crearCajon(20L, estacionamiento);
        Reserva reserva = crearReserva(cliente, estacionamiento, cajon, EstadoReserva.CREADA);
        reserva.setFechaExpiracion(LocalDateTime.now().minusMinutes(1));

        when(usuarioRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(operador));
        when(reservaRepository.findByCodigoAndActivoTrue("RSV-ABC12345")).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() ->
                ticketService.registrarEntrada(2L, new TicketEntradaRequest("RSV-ABC12345"))
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage("La reserva ya expiro y no puede convertirse en ticket.");

        verify(ticketRepository, never()).save(any());
    }

    /**
     * Verifica que un usuario sin rol operativo no pueda registrar entradas.
     */
    @Test
    void debeRechazarCuandoUsuarioNoTieneRolOperativo() {
        Estacionamiento estacionamiento = crearEstacionamiento(10L);
        Usuario cliente = crearUsuario(1L, "USER");
        Usuario operador = crearUsuario(2L, "USER");
        Cajon cajon = crearCajon(20L, estacionamiento);
        Reserva reserva = crearReserva(cliente, estacionamiento, cajon, EstadoReserva.CREADA);

        when(usuarioRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(operador));
        when(reservaRepository.findByCodigoAndActivoTrue("RSV-ABC12345")).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() ->
                ticketService.registrarEntrada(2L, new TicketEntradaRequest("RSV-ABC12345"))
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage("El usuario autenticado no puede operar tickets de este estacionamiento.");

        verify(ticketRepository, never()).save(any());
    }

    /**
     * Verifica que un operador no asignado al estacionamiento no pueda registrar la entrada.
     */
    @Test
    void debeRechazarCuandoOperadorNoEstaAsignadoAlEstacionamiento() {
        Estacionamiento estacionamiento = crearEstacionamiento(10L);
        Usuario cliente = crearUsuario(1L, "USER");
        Usuario operador = crearUsuario(2L, "OPERADOR");
        Cajon cajon = crearCajon(20L, estacionamiento);
        Reserva reserva = crearReserva(cliente, estacionamiento, cajon, EstadoReserva.CREADA);

        when(usuarioRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(operador));
        when(reservaRepository.findByCodigoAndActivoTrue("RSV-ABC12345")).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() ->
                ticketService.registrarEntrada(2L, new TicketEntradaRequest("RSV-ABC12345"))
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage("El usuario autenticado no puede operar tickets de este estacionamiento.");

        verify(ticketRepository, never()).save(any());
    }

    /**
     * Verifica que una reserva ya convertida no genere un segundo ticket.
     */
    @Test
    void debeRechazarCuandoReservaYaTieneTicket() {
        Estacionamiento estacionamiento = crearEstacionamiento(10L);
        Usuario cliente = crearUsuario(1L, "USER");
        Usuario operador = crearUsuario(2L, "OPERADOR");
        operador.getEstacionamientos().add(estacionamiento);
        Cajon cajon = crearCajon(20L, estacionamiento);
        Reserva reserva = crearReserva(cliente, estacionamiento, cajon, EstadoReserva.CREADA);

        when(usuarioRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(operador));
        when(reservaRepository.findByCodigoAndActivoTrue("RSV-ABC12345")).thenReturn(Optional.of(reserva));
        when(ticketRepository.existsByReservaIdAndActivoTrue(30L)).thenReturn(true);

        assertThatThrownBy(() ->
                ticketService.registrarEntrada(2L, new TicketEntradaRequest("RSV-ABC12345"))
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage("La reserva ya fue convertida en ticket.");

        verify(ticketRepository, never()).save(any());
    }

    /**
     * Verifica que no se genere ticket si el cajon ya tiene otro ticket abierto.
     */
    @Test
    void debeRechazarCuandoCajonYaTieneTicketAbierto() {
        Estacionamiento estacionamiento = crearEstacionamiento(10L);
        Usuario cliente = crearUsuario(1L, "USER");
        Usuario operador = crearUsuario(2L, "OPERADOR");
        operador.getEstacionamientos().add(estacionamiento);
        Cajon cajon = crearCajon(20L, estacionamiento);
        Reserva reserva = crearReserva(cliente, estacionamiento, cajon, EstadoReserva.CREADA);

        when(usuarioRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(operador));
        when(reservaRepository.findByCodigoAndActivoTrue("RSV-ABC12345")).thenReturn(Optional.of(reserva));
        when(ticketRepository.existsByReservaIdAndActivoTrue(30L)).thenReturn(false);
        when(ticketRepository.existsByCajonIdAndEstadoAndActivoTrue(20L, EstadoTicket.ABIERTO))
                .thenReturn(true);

        assertThatThrownBy(() ->
                ticketService.registrarEntrada(2L, new TicketEntradaRequest("RSV-ABC12345"))
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage("El cajon ya tiene un ticket abierto.");

        verify(ticketRepository, never()).save(any());
    }

    /**
     * Verifica que ADMIN pueda registrar entrada en cualquier estacionamiento.
     */
    @Test
    void debeRegistrarEntradaConAdmin() {
        Estacionamiento estacionamiento = crearEstacionamiento(10L);
        Usuario cliente = crearUsuario(1L, "USER");
        Usuario admin = crearUsuario(2L, "ADMIN");
        Cajon cajon = crearCajon(20L, estacionamiento);
        Reserva reserva = crearReserva(cliente, estacionamiento, cajon, EstadoReserva.CREADA);
        Ticket ticket = crearTicket(40L, reserva, cliente, admin, estacionamiento, cajon);
        TicketResponse response = crearResponse();

        when(usuarioRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(admin));
        when(reservaRepository.findByCodigoAndActivoTrue("RSV-ABC12345")).thenReturn(Optional.of(reserva));
        when(ticketRepository.existsByReservaIdAndActivoTrue(30L)).thenReturn(false);
        when(ticketRepository.existsByCajonIdAndEstadoAndActivoTrue(20L, EstadoTicket.ABIERTO)).thenReturn(false);
        when(ticketRepository.findByCodigoAndActivoTrue(anyString())).thenReturn(Optional.empty());
        when(ticketMapper.toEntity(
                anyString(),
                eq("ABC123"),
                any(LocalDateTime.class),
                eq(reserva),
                eq(cliente),
                eq(admin),
                eq(estacionamiento),
                eq(cajon)
        )).thenReturn(ticket);
        when(cajonRepository.save(cajon)).thenReturn(cajon);
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        TicketResponse resultado =
                ticketService.registrarEntrada(2L, new TicketEntradaRequest("RSV-ABC12345"));

        assertThat(resultado).isEqualTo(response);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.USADA);
        assertThat(cajon.getEstado()).isEqualTo(EstadoCajon.OCUPADO);
    }

    /**
     * Verifica que OWNER pueda registrar entrada solo en un estacionamiento propio.
     */
    @Test
    void debeRegistrarEntradaConOwnerDelEstacionamiento() {
        Usuario owner = crearUsuario(2L, "OWNER");
        Estacionamiento estacionamiento = crearEstacionamiento(10L);
        estacionamiento.setOwner(owner);
        Usuario cliente = crearUsuario(1L, "USER");
        Cajon cajon = crearCajon(20L, estacionamiento);
        Reserva reserva = crearReserva(cliente, estacionamiento, cajon, EstadoReserva.CREADA);
        Ticket ticket = crearTicket(40L, reserva, cliente, owner, estacionamiento, cajon);
        TicketResponse response = crearResponse();

        when(usuarioRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(owner));
        when(reservaRepository.findByCodigoAndActivoTrue("RSV-ABC12345")).thenReturn(Optional.of(reserva));
        when(ticketRepository.existsByReservaIdAndActivoTrue(30L)).thenReturn(false);
        when(ticketRepository.existsByCajonIdAndEstadoAndActivoTrue(20L, EstadoTicket.ABIERTO)).thenReturn(false);
        when(ticketRepository.findByCodigoAndActivoTrue(anyString())).thenReturn(Optional.empty());
        when(ticketMapper.toEntity(
                anyString(),
                eq("ABC123"),
                any(LocalDateTime.class),
                eq(reserva),
                eq(cliente),
                eq(owner),
                eq(estacionamiento),
                eq(cajon)
        )).thenReturn(ticket);
        when(cajonRepository.save(cajon)).thenReturn(cajon);
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        TicketResponse resultado =
                ticketService.registrarEntrada(2L, new TicketEntradaRequest("RSV-ABC12345"));

        assertThat(resultado).isEqualTo(response);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.USADA);
        assertThat(cajon.getEstado()).isEqualTo(EstadoCajon.OCUPADO);
    }

    /**
     * Verifica que se cierre un ticket abierto y se libere el cajon.
     */
    @Test
    void debeRegistrarSalidaYCambiarEstados() {
        Estacionamiento estacionamiento = crearEstacionamiento(10L);
        Usuario cliente = crearUsuario(1L, "USER");
        Usuario operador = crearUsuario(2L, "OPERADOR");
        operador.getEstacionamientos().add(estacionamiento);
        Cajon cajon = crearCajon(20L, estacionamiento);
        cajon.setEstado(EstadoCajon.OCUPADO);
        Reserva reserva = crearReserva(cliente, estacionamiento, cajon, EstadoReserva.USADA);
        Ticket ticket = crearTicket(40L, reserva, cliente, operador, estacionamiento, cajon);
        TicketResponse response = crearResponseCerrado();

        when(usuarioRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(operador));
        when(ticketRepository.findByIdAndActivoTrue(40L)).thenReturn(Optional.of(ticket));
        when(cajonRepository.save(cajon)).thenReturn(cajon);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        TicketResponse resultado = ticketService.registrarSalida(2L, 40L);

        assertThat(resultado).isEqualTo(response);
        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.CERRADO);
        assertThat(ticket.getFechaSalida()).isNotNull();
        assertThat(cajon.getEstado()).isEqualTo(EstadoCajon.LIBRE);
        verify(cajonRepository).save(cajon);
        verify(ticketRepository).save(ticket);
    }

    /**
     * Verifica que no se pueda cerrar dos veces el mismo ticket.
     */
    @Test
    void debeRechazarSalidaCuandoTicketYaEstaCerrado() {
        Estacionamiento estacionamiento = crearEstacionamiento(10L);
        Usuario cliente = crearUsuario(1L, "USER");
        Usuario operador = crearUsuario(2L, "OPERADOR");
        operador.getEstacionamientos().add(estacionamiento);
        Cajon cajon = crearCajon(20L, estacionamiento);
        Reserva reserva = crearReserva(cliente, estacionamiento, cajon, EstadoReserva.USADA);
        Ticket ticket = crearTicket(40L, reserva, cliente, operador, estacionamiento, cajon);
        ticket.setEstado(EstadoTicket.CERRADO);

        when(usuarioRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(operador));
        when(ticketRepository.findByIdAndActivoTrue(40L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.registrarSalida(2L, 40L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Solo se puede registrar salida para tickets en estado ABIERTO.");

        verify(cajonRepository, never()).save(any());
        verify(ticketRepository, never()).save(any());
    }

    /**
     * Verifica que un usuario sin alcance no pueda registrar la salida.
     */
    @Test
    void debeRechazarSalidaCuandoUsuarioNoPuedeOperarTicket() {
        Estacionamiento estacionamiento = crearEstacionamiento(10L);
        Usuario cliente = crearUsuario(1L, "USER");
        Usuario operador = crearUsuario(2L, "OPERADOR");
        Cajon cajon = crearCajon(20L, estacionamiento);
        Reserva reserva = crearReserva(cliente, estacionamiento, cajon, EstadoReserva.USADA);
        Ticket ticket = crearTicket(40L, reserva, cliente, operador, estacionamiento, cajon);

        when(usuarioRepository.findByIdAndActivoTrue(2L)).thenReturn(Optional.of(operador));
        when(ticketRepository.findByIdAndActivoTrue(40L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.registrarSalida(2L, 40L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El usuario autenticado no puede operar tickets de este estacionamiento.");

        verify(cajonRepository, never()).save(any());
        verify(ticketRepository, never()).save(any());
    }

    /**
     * Crea un usuario de prueba con un rol funcional.
     */
    private Usuario crearUsuario(Long id, String rolNombre) {
        Rol rol = Rol.builder()
                .nombre(rolNombre)
                .build();
        rol.setId(id + 100);

        Usuario usuario = Usuario.builder()
                .nombre("Usuario")
                .apellido("Prueba")
                .email("usuario" + id + "@parkio.com")
                .passwordHash("hash")
                .build();
        usuario.setId(id);
        usuario.setActivo(true);
        usuario.getRoles().add(rol);
        return usuario;
    }

    /**
     * Crea un estacionamiento activo para asociarlo a reserva, operador y ticket.
     */
    private Estacionamiento crearEstacionamiento(Long id) {
        Estacionamiento estacionamiento = Estacionamiento.builder()
                .nombre("Estacionamiento")
                .build();
        estacionamiento.setId(id);
        estacionamiento.setActivo(true);
        return estacionamiento;
    }

    /**
     * Crea un cajon activo para validar el cambio a OCUPADO.
     */
    private Cajon crearCajon(Long id, Estacionamiento estacionamiento) {
        Cajon cajon = Cajon.builder()
                .numero("A-01")
                .tipo(TipoCajon.AUTO)
                .estado(EstadoCajon.RESERVADO)
                .estacionamiento(estacionamiento)
                .build();
        cajon.setId(id);
        cajon.setActivo(true);
        return cajon;
    }

    /**
     * Crea una reserva base con expiracion futura para escenarios exitosos y de validacion.
     */
    private Reserva crearReserva(
            Usuario usuario,
            Estacionamiento estacionamiento,
            Cajon cajon,
            EstadoReserva estado
    ) {
        Reserva reserva = Reserva.builder()
                .codigo("RSV-ABC12345")
                .placa("ABC123")
                .estado(estado)
                .fechaReserva(LocalDateTime.now().minusMinutes(5))
                .fechaExpiracion(LocalDateTime.now().plusMinutes(15))
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
     * Crea un ticket simulado que representa el registro persistido.
     */
    private Ticket crearTicket(
            Long id,
            Reserva reserva,
            Usuario usuario,
            Usuario operador,
            Estacionamiento estacionamiento,
            Cajon cajon
    ) {
        Ticket ticket = Ticket.builder()
                .codigo("TCK-ABC12345")
                .estado(EstadoTicket.ABIERTO)
                .placa("ABC123")
                .fechaEntrada(LocalDateTime.now())
                .reserva(reserva)
                .usuario(usuario)
                .operadorEntrada(operador)
                .estacionamiento(estacionamiento)
                .cajon(cajon)
                .build();
        ticket.setId(id);
        ticket.setActivo(true);
        return ticket;
    }

    /**
     * Crea el DTO esperado por el service despues de mapear el ticket guardado.
     */
    private TicketResponse crearResponse() {
        return new TicketResponse(
                40L,
                "TCK-ABC12345",
                EstadoTicket.ABIERTO,
                "ABC123",
                LocalDateTime.of(2026, 7, 25, 10, 0),
                null,
                30L,
                1L,
                2L,
                10L,
                20L,
                true,
                LocalDateTime.of(2026, 7, 25, 10, 1)
        );
    }

    /**
     * Crea el DTO esperado cuando el ticket queda cerrado.
     */
    private TicketResponse crearResponseCerrado() {
        return new TicketResponse(
                40L,
                "TCK-ABC12345",
                EstadoTicket.CERRADO,
                "ABC123",
                LocalDateTime.of(2026, 7, 25, 10, 0),
                LocalDateTime.of(2026, 7, 25, 11, 0),
                30L,
                1L,
                2L,
                10L,
                20L,
                true,
                LocalDateTime.of(2026, 7, 25, 10, 1)
        );
    }
}
