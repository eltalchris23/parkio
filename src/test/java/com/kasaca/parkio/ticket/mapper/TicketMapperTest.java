package com.kasaca.parkio.ticket.mapper;

import com.kasaca.parkio.cajon.entity.Cajon;
import com.kasaca.parkio.cajon.entity.EstadoCajon;
import com.kasaca.parkio.cajon.entity.TipoCajon;
import com.kasaca.parkio.estacionamiento.entity.Estacionamiento;
import com.kasaca.parkio.reserva.entity.EstadoReserva;
import com.kasaca.parkio.reserva.entity.Reserva;
import com.kasaca.parkio.ticket.dto.TicketResponse;
import com.kasaca.parkio.ticket.entity.EstadoTicket;
import com.kasaca.parkio.ticket.entity.Ticket;
import com.kasaca.parkio.usuario.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TicketMapperTest {

    private TicketMapper ticketMapper;

    /**
     * Configura una instancia real del mapper porque no depende de Spring ni de repositorios.
     */
    @BeforeEach
    void setUp() {
        ticketMapper = new TicketMapper();
    }

    /**
     * Verifica que el mapper construya un ticket nuevo en estado ABIERTO.
     */
    @Test
    void debeConvertirDatosDeEntradaAEntidad() {
        LocalDateTime fechaEntrada = LocalDateTime.of(2026, 7, 25, 10, 0);
        Usuario usuario = crearUsuario(1L);
        Usuario operador = crearUsuario(2L);
        Estacionamiento estacionamiento = crearEstacionamiento(10L);
        Cajon cajon = crearCajon(20L, estacionamiento);
        Reserva reserva = crearReserva(30L, usuario, estacionamiento, cajon);

        Ticket resultado = ticketMapper.toEntity(
                "TCK-ABC12345",
                "ABC123",
                fechaEntrada,
                reserva,
                usuario,
                operador,
                estacionamiento,
                cajon
        );

        assertThat(resultado.getCodigo()).isEqualTo("TCK-ABC12345");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(resultado.getPlaca()).isEqualTo("ABC123");
        assertThat(resultado.getFechaEntrada()).isEqualTo(fechaEntrada);
        assertThat(resultado.getFechaSalida()).isNull();
        assertThat(resultado.getReserva()).isEqualTo(reserva);
        assertThat(resultado.getUsuario()).isEqualTo(usuario);
        assertThat(resultado.getOperadorEntrada()).isEqualTo(operador);
        assertThat(resultado.getEstacionamiento()).isEqualTo(estacionamiento);
        assertThat(resultado.getCajon()).isEqualTo(cajon);
    }

    /**
     * Verifica que el mapper exponga en el response solo datos seguros e identificadores.
     */
    @Test
    void debeConvertirEntidadAResponse() {
        LocalDateTime fechaEntrada = LocalDateTime.of(2026, 7, 25, 10, 0);
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 7, 25, 10, 1);
        Usuario usuario = crearUsuario(1L);
        Usuario operador = crearUsuario(2L);
        Estacionamiento estacionamiento = crearEstacionamiento(10L);
        Cajon cajon = crearCajon(20L, estacionamiento);
        Reserva reserva = crearReserva(30L, usuario, estacionamiento, cajon);
        Ticket ticket = crearTicket(40L, reserva, usuario, operador, estacionamiento, cajon);
        ticket.setFechaEntrada(fechaEntrada);
        ticket.setFechaCreacion(fechaCreacion);

        TicketResponse resultado = ticketMapper.toResponse(ticket);

        assertThat(resultado.id()).isEqualTo(40L);
        assertThat(resultado.codigo()).isEqualTo("TCK-ABC12345");
        assertThat(resultado.estado()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(resultado.placa()).isEqualTo("ABC123");
        assertThat(resultado.fechaEntrada()).isEqualTo(fechaEntrada);
        assertThat(resultado.fechaSalida()).isNull();
        assertThat(resultado.reservaId()).isEqualTo(30L);
        assertThat(resultado.usuarioId()).isEqualTo(1L);
        assertThat(resultado.operadorEntradaId()).isEqualTo(2L);
        assertThat(resultado.estacionamientoId()).isEqualTo(10L);
        assertThat(resultado.cajonId()).isEqualTo(20L);
        assertThat(resultado.activo()).isTrue();
        assertThat(resultado.fechaCreacion()).isEqualTo(fechaCreacion);
    }

    /**
     * Crea un usuario minimo para relacionarlo con reservas y tickets.
     */
    private Usuario crearUsuario(Long id) {
        Usuario usuario = Usuario.builder()
                .nombre("Usuario")
                .apellido("Prueba")
                .email("usuario" + id + "@parkio.com")
                .passwordHash("hash")
                .build();
        usuario.setId(id);
        usuario.setActivo(true);
        return usuario;
    }

    /**
     * Crea un estacionamiento minimo para relacionarlo con cajones, reservas y tickets.
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
     * Crea un cajon minimo con estado LIBRE para usarlo como dependencia del ticket.
     */
    private Cajon crearCajon(Long id, Estacionamiento estacionamiento) {
        Cajon cajon = Cajon.builder()
                .numero("A-01")
                .tipo(TipoCajon.AUTO)
                .estado(EstadoCajon.LIBRE)
                .estacionamiento(estacionamiento)
                .build();
        cajon.setId(id);
        cajon.setActivo(true);
        return cajon;
    }

    /**
     * Crea una reserva vigente que sirve como origen funcional del ticket.
     */
    private Reserva crearReserva(
            Long id,
            Usuario usuario,
            Estacionamiento estacionamiento,
            Cajon cajon
    ) {
        Reserva reserva = Reserva.builder()
                .codigo("RSV-ABC12345")
                .placa("ABC123")
                .estado(EstadoReserva.CREADA)
                .fechaReserva(LocalDateTime.of(2026, 7, 25, 9, 50))
                .fechaExpiracion(LocalDateTime.of(2026, 7, 25, 10, 20))
                .tiempoExpiracionMinutos(20)
                .usuario(usuario)
                .estacionamiento(estacionamiento)
                .cajon(cajon)
                .build();
        reserva.setId(id);
        reserva.setActivo(true);
        return reserva;
    }

    /**
     * Crea un ticket ABIERTO completo para validar la conversion a response.
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
}
