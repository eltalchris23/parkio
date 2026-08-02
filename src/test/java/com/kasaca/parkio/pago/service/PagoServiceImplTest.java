package com.kasaca.parkio.pago.service;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.repository.CajonRepository;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.pago.dto.PagoRequest;
import com.kasaca.parkio.pago.dto.PagoResponse;
import com.kasaca.parkio.pago.entity.MetodoPago;
import com.kasaca.parkio.pago.entity.Pago;
import com.kasaca.parkio.pago.mapper.PagoMapper;
import com.kasaca.parkio.pago.repository.PagoRepository;
import com.kasaca.parkio.shared.dto.PageResponse;
import com.kasaca.parkio.rol.entity.Rol;
import com.kasaca.parkio.shared.exception.ConflictException;
import com.kasaca.parkio.ticket.entity.EstadoTicket;
import com.kasaca.parkio.ticket.entity.Ticket;
import com.kasaca.parkio.ticket.repository.TicketRepository;
import com.kasaca.parkio.usuario.entity.Usuario;
import com.kasaca.parkio.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CajonRepository cajonRepository;

    @Mock
    private PagoMapper pagoMapper;

    @InjectMocks
    private PagoServiceImpl pagoService;

    /**
     * Verifica que ADMIN pueda listar pagos activos con filtros y paginacion.
     */
    @Test
    void debeListarPagosConAdmin() {
        Usuario admin = crearUsuario(1L, "ADMIN");
        Ticket ticket = crearTicketPendientePago();
        Pago pago = crearPago(ticket, admin, new BigDecimal("27.50"));
        PagoResponse response = crearResponse();
        Pageable pageable = PageRequest.of(0, 10);

        when(usuarioRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(admin));
        when(pagoRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Pago>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pago), pageable, 1));
        when(pagoMapper.toResponse(pago)).thenReturn(response);

        PageResponse<PagoResponse> resultado = pagoService.getPagos(
                1L,
                1L,
                MetodoPago.EFECTIVO,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                pageable
        );

        assertThat(resultado.content()).containsExactly(response);
        assertThat(resultado.totalElements()).isEqualTo(1);
        verify(pagoRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Pago>>any(), any(Pageable.class));
    }

    /**
     * Verifica que OPERADOR sin estacionamientos asignados reciba pagina vacia.
     */
    @Test
    void debeListarPaginaVaciaCuandoOperadorNoTieneEstacionamientos() {
        Usuario operador = crearUsuario(9L, "OPERADOR");
        Pageable pageable = PageRequest.of(0, 10);

        when(usuarioRepository.findByIdAndActivoTrue(9L)).thenReturn(Optional.of(operador));

        PageResponse<PagoResponse> resultado = pagoService.getPagos(
                9L,
                null,
                null,
                null,
                null,
                pageable
        );

        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isZero();
        verify(pagoRepository, never())
                .findAll(org.mockito.ArgumentMatchers.<Specification<Pago>>any(), any(Pageable.class));
    }

    /**
     * Verifica que se rechace un rango donde fechaInicio sea posterior a fechaFin.
     */
    @Test
    void debeRechazarListadoConRangoFechasInvalido() {
        Usuario admin = crearUsuario(1L, "ADMIN");

        when(usuarioRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> pagoService.getPagos(
                1L,
                null,
                null,
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 8, 1),
                PageRequest.of(0, 10)
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("La fecha de inicio no puede ser posterior a la fecha fin.");

        verify(pagoRepository, never())
                .findAll(org.mockito.ArgumentMatchers.<Specification<Pago>>any(), any(Pageable.class));
    }

    /**
     * Verifica que ADMIN pueda registrar un pago, cerrar el ticket y liberar el cajon.
     */
    @Test
    void debeRegistrarPagoConAdmin() {
        Usuario admin = crearUsuario(1L, "ADMIN");
        Ticket ticket = crearTicketPendientePago();
        PagoRequest request = crearRequest(new BigDecimal("100.00"));
        Pago pago = crearPago(ticket, admin, new BigDecimal("27.50"));
        PagoResponse response = crearResponse();

        when(usuarioRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(admin));
        when(ticketRepository.findByIdAndActivoTrue(3L)).thenReturn(Optional.of(ticket));
        when(pagoRepository.existsByTicketIdAndActivoTrue(3L)).thenReturn(false);
        when(pagoMapper.toEntity(any(), any(), any(), any(), any(), any())).thenReturn(pago);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(cajonRepository.save(ticket.getCajon())).thenReturn(ticket.getCajon());
        when(pagoRepository.save(pago)).thenReturn(pago);
        when(pagoMapper.toResponse(pago)).thenReturn(response);

        PagoResponse resultado = pagoService.registrarPago(1L, request);

        assertThat(resultado).isEqualTo(response);
        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.CERRADO);
        assertThat(ticket.getCajon().getEstado()).isEqualTo(EstadoCajon.LIBRE);

        ArgumentCaptor<BigDecimal> cambioCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(pagoMapper).toEntity(
                any(Ticket.class),
                any(Usuario.class),
                any(BigDecimal.class),
                cambioCaptor.capture(),
                any(MetodoPago.class),
                any(LocalDateTime.class)
        );
        assertThat(cambioCaptor.getValue()).isEqualByComparingTo("27.50");
        verify(ticketRepository).save(ticket);
        verify(cajonRepository).save(ticket.getCajon());
        verify(pagoRepository).save(pago);
    }

    /**
     * Verifica que el pago exacto sea valido y genere cambio cero.
     */
    @Test
    void debeRegistrarPagoExactoConCambioCero() {
        Usuario operador = crearUsuario(9L, "OPERADOR");
        Estacionamiento estacionamiento = crearEstacionamiento();
        operador.setEstacionamientos(Set.of(estacionamiento));
        Ticket ticket = crearTicketPendientePago();
        ticket.setEstacionamiento(estacionamiento);
        PagoRequest request = crearRequest(new BigDecimal("72.50"));
        Pago pago = crearPago(ticket, operador, BigDecimal.ZERO);
        PagoResponse response = crearResponse();

        when(usuarioRepository.findByIdAndActivoTrue(9L)).thenReturn(Optional.of(operador));
        when(ticketRepository.findByIdAndActivoTrue(3L)).thenReturn(Optional.of(ticket));
        when(pagoRepository.existsByTicketIdAndActivoTrue(3L)).thenReturn(false);
        when(pagoMapper.toEntity(any(), any(), any(), any(), any(), any())).thenReturn(pago);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(cajonRepository.save(ticket.getCajon())).thenReturn(ticket.getCajon());
        when(pagoRepository.save(pago)).thenReturn(pago);
        when(pagoMapper.toResponse(pago)).thenReturn(response);

        PagoResponse resultado = pagoService.registrarPago(9L, request);

        assertThat(resultado).isEqualTo(response);
        ArgumentCaptor<BigDecimal> cambioCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(pagoMapper).toEntity(
                any(Ticket.class),
                any(Usuario.class),
                any(BigDecimal.class),
                cambioCaptor.capture(),
                any(MetodoPago.class),
                any(LocalDateTime.class)
        );
        assertThat(cambioCaptor.getValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * Verifica que no se permita pagar un ticket que aun no esta pendiente de pago.
     */
    @Test
    void debeRechazarPagoCuandoTicketNoEstaPendiente() {
        Usuario admin = crearUsuario(1L, "ADMIN");
        Ticket ticket = crearTicketPendientePago();
        ticket.setEstado(EstadoTicket.ABIERTO);

        when(usuarioRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(admin));
        when(ticketRepository.findByIdAndActivoTrue(3L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> pagoService.registrarPago(1L, crearRequest(new BigDecimal("100.00"))))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Solo se pueden pagar tickets en estado PENDIENTE_PAGO.");

        verify(pagoRepository, never()).save(any());
        verify(ticketRepository, never()).save(any());
        verify(cajonRepository, never()).save(any());
    }

    /**
     * Verifica que el service rechace pagos con monto recibido menor al total calculado.
     */
    @Test
    void debeRechazarPagoConMontoInsuficiente() {
        Usuario admin = crearUsuario(1L, "ADMIN");
        Ticket ticket = crearTicketPendientePago();

        when(usuarioRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(admin));
        when(ticketRepository.findByIdAndActivoTrue(3L)).thenReturn(Optional.of(ticket));
        when(pagoRepository.existsByTicketIdAndActivoTrue(3L)).thenReturn(false);

        assertThatThrownBy(() -> pagoService.registrarPago(1L, crearRequest(new BigDecimal("50.00"))))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El monto recibido es menor al monto total del ticket.");

        verify(pagoRepository, never()).save(any());
        verify(ticketRepository, never()).save(any());
        verify(cajonRepository, never()).save(any());
    }

    /**
     * Verifica que no se permita registrar un segundo pago activo para el mismo ticket.
     */
    @Test
    void debeRechazarPagoDuplicado() {
        Usuario admin = crearUsuario(1L, "ADMIN");
        Ticket ticket = crearTicketPendientePago();

        when(usuarioRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(admin));
        when(ticketRepository.findByIdAndActivoTrue(3L)).thenReturn(Optional.of(ticket));
        when(pagoRepository.existsByTicketIdAndActivoTrue(3L)).thenReturn(true);

        assertThatThrownBy(() -> pagoService.registrarPago(1L, crearRequest(new BigDecimal("100.00"))))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El ticket ya tiene un pago registrado.");

        verify(pagoRepository, never()).save(any());
    }

    /**
     * Verifica que USER no pueda registrar pagos aunque conozca el ticket.
     */
    @Test
    void debeRechazarRegistroPagoConUser() {
        Usuario user = crearUsuario(7L, "USER");
        Ticket ticket = crearTicketPendientePago();
        ticket.setUsuario(user);

        when(usuarioRepository.findByIdAndActivoTrue(7L)).thenReturn(Optional.of(user));
        when(ticketRepository.findByIdAndActivoTrue(3L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> pagoService.registrarPago(7L, crearRequest(new BigDecimal("100.00"))))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El usuario autenticado no puede registrar pagos de este estacionamiento.");

        verify(pagoRepository, never()).save(any());
    }

    /**
     * Verifica que el cliente pueda consultar el pago de su propio ticket.
     */
    @Test
    void debeConsultarPagoDelUsuarioPropio() {
        Usuario user = crearUsuario(7L, "USER");
        Ticket ticket = crearTicketPendientePago();
        ticket.setUsuario(user);
        Pago pago = crearPago(ticket, crearUsuario(9L, "OPERADOR"), new BigDecimal("27.50"));
        PagoResponse response = crearResponse();

        when(usuarioRepository.findByIdAndActivoTrue(7L)).thenReturn(Optional.of(user));
        when(ticketRepository.findByIdAndActivoTrue(3L)).thenReturn(Optional.of(ticket));
        when(pagoRepository.findByTicketIdAndActivoTrue(3L)).thenReturn(Optional.of(pago));
        when(pagoMapper.toResponse(pago)).thenReturn(response);

        PagoResponse resultado = pagoService.getPagoByTicketId(7L, 3L);

        assertThat(resultado).isEqualTo(response);
        verify(pagoRepository).findByTicketIdAndActivoTrue(3L);
    }

    /**
     * Construye un request valido para registrar pago.
     */
    private PagoRequest crearRequest(BigDecimal montoRecibido) {
        return new PagoRequest(3L, montoRecibido, MetodoPago.EFECTIVO);
    }

    /**
     * Construye un ticket pendiente de pago con cajon ocupado y monto calculado.
     */
    private Ticket crearTicketPendientePago() {
        Ticket ticket = new Ticket();
        ticket.setId(3L);
        ticket.setCodigo("TCK-ABC12345");
        ticket.setEstado(EstadoTicket.PENDIENTE_PAGO);
        ticket.setMontoTotal(new BigDecimal("72.50"));
        ticket.setUsuario(crearUsuario(7L, "USER"));
        ticket.setEstacionamiento(crearEstacionamiento());
        ticket.setCajon(crearCajon());
        ticket.setActivo(true);
        return ticket;
    }

    /**
     * Construye un pago asociado al ticket.
     */
    private Pago crearPago(Ticket ticket, Usuario operador, BigDecimal cambio) {
        Pago pago = new Pago();
        pago.setId(5L);
        pago.setTicket(ticket);
        pago.setOperador(operador);
        pago.setMontoTotal(ticket.getMontoTotal());
        pago.setMontoRecibido(new BigDecimal("100.00"));
        pago.setCambio(cambio);
        pago.setMetodoPago(MetodoPago.EFECTIVO);
        pago.setActivo(true);
        return pago;
    }

    /**
     * Construye el response esperado para el pago.
     */
    private PagoResponse crearResponse() {
        return new PagoResponse(
                5L,
                3L,
                "TCK-ABC12345",
                new BigDecimal("72.50"),
                new BigDecimal("100.00"),
                new BigDecimal("27.50"),
                "EFECTIVO",
                "REGISTRADO",
                LocalDateTime.of(2026, 8, 1, 18, 30),
                9L,
                true,
                LocalDateTime.of(2026, 8, 1, 18, 30)
        );
    }

    /**
     * Construye un usuario con un rol activo.
     */
    private Usuario crearUsuario(Long id, String rolNombre) {
        Rol rol = new Rol();
        rol.setId(id);
        rol.setNombre(rolNombre);
        rol.setActivo(true);

        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setActivo(true);
        usuario.setRoles(new HashSet<>(Set.of(rol)));
        usuario.setEstacionamientos(new HashSet<>());
        return usuario;
    }

    /**
     * Construye un estacionamiento activo.
     */
    private Estacionamiento crearEstacionamiento() {
        Estacionamiento estacionamiento = new Estacionamiento();
        estacionamiento.setId(1L);
        estacionamiento.setActivo(true);
        return estacionamiento;
    }

    /**
     * Construye un cajon ocupado para validar que el pago lo libere.
     */
    private Cajon crearCajon() {
        Cajon cajon = new Cajon();
        cajon.setId(2L);
        cajon.setEstado(EstadoCajon.OCUPADO);
        cajon.setActivo(true);
        return cajon;
    }
}
